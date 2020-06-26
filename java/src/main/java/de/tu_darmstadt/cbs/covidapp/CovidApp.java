package de.tu_darmstadt.cbs.covidapp;

import de.tu_darmstadt.cbs.emailsmpc.*;

import java.math.BigInteger;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CovidApp {
    private Configuration config;
    private AppModel model;
    private Date lastUpdate;
    private BigInteger[] lastResult;
    private ReadWriteLock modelLock;
    private ReadWriteLock resultLock;
    private NetworkListener networkListener;
    private NetworkSender networkSender;
    public volatile CountDownLatch initialized;
    public volatile CountDownLatch allClientsInitialized; // If host, count down until all Ack. if client set to 0 if run signal
    public volatile CountDownLatch sumComputable;
    public volatile CountDownLatch resultComputable;
    public volatile CountDownLatch rerunComputation;
    public volatile int ownId;
    public volatile ResultMessageBuffer resultMessageBuffer;
    private Timer rerunTimer;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java -jar covidApp.jar <configfile.toml>");
            System.exit(-1);
        }
        try {
            Configuration config = TOMLConfigReader.getConfig(new File(args[0]));
            CovidApp app = new CovidApp(config);
            try {
                app.runNetworkListener();
                app.runRestServer();
            } catch (UnrecoverableKeyException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException
                    | CertificateException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            if (config.host == true) {
                app.initializeHostModel();
                app.initializeNetworkSender();
                app.sendMessages(MessageType.INITIAL);
                app.allClientsInitialized.await();
                app.allClientsInitialized = new CountDownLatch(config.participants.length -1);
                try {
                    app.modelLock.readLock().tryLock(5, TimeUnit.SECONDS);
                    app.model.toContinuousFromIntialSending();
                } catch (InterruptedException e) {
                    System.err.println("Locking of ReadLock modelLock timed out! " + e);
                    System.exit(-1);
                } finally {
                    app.modelLock.readLock().unlock();
                }
                System.out.println("Sent all initialMessages");
                app.sendMessages(MessageType.DONE_INIT);

            } else { // Not host
                app.initializeClientModel();
                app.initialized.await();
                System.out.println("Model Initialized");
                app.initializeNetworkSender();
                app.sendAck();
                app.shareData(app.readData());
                // switch to continuous is in ConnectionHandler
                app.allClientsInitialized.await();
                System.out.println("All clients initialized, starting computation");
                app.sendMessages(MessageType.SHARE);
            }
            app.mainLoop();
        } catch (IllegalStateException e) {
            System.err.println("Error in config or data file: " + e);
            System.exit(-1);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error reading file: " + e);
            System.exit(-1);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void writeLockModel() {
        try {
            modelLock.writeLock().tryLock(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println("Locking of WriteLock modelLock timed out! " + e);
            System.exit(-1);
        }
    }

    public void writeUnlockModel() {
        modelLock.writeLock().unlock();
    }

    public void readLockModel() {
        try {
            modelLock.readLock().tryLock(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println("Locking of ReadLock modelLock timed out! " + e);
            System.exit(-1);
        }
    }

    public void readUnlockModel() {
        modelLock.readLock().unlock();
    }

    public AppModel getModel() {
        return model;
    }
    public void mainLoop() {
    	for (;;) {
            try {
				sumComputable.await();
				sumComputable = new CountDownLatch(1);
				model.populateResultMessages();
			
				sendMessages(MessageType.RESULT);
				resultComputable.await();
				resultComputable = new CountDownLatch(1);
				transferMessageBufferToBins();
				resultMessageBuffer.clear();
				setNewResult(getResult());
				resetModel();
				if (config.host && config.periodicRebuild == true && rerunTimer == null)
					setTimer();
				rerunComputation.await();
			
            rerunComputation = new CountDownLatch(1);
            shareData(readData());
            if (config.host) {
            	sendMessages(MessageType.RECALC);
            	allClientsInitialized.await();
                allClientsInitialized = new CountDownLatch(config.participants.length -1);
            }
            } catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
            } catch (IllegalStateException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	sendMessages(MessageType.SHARE);
        }
    }
    
    public boolean isHost() {
    	return config.host;
    }
    
    public BigInteger[] readData() {
    	try {
            modelLock.writeLock().tryLock(5, TimeUnit.SECONDS);
            HistogramImporter importer = new CSVImporter(config.dataFile, config.binNames.length);
            BigInteger[] data = importer.getCounts();
            return data;
        } catch (InterruptedException e) {
            System.err.println("Locking of WriteLock modelLock timed out! " + e);
            System.exit(-1);
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
            modelLock.writeLock().unlock();
        }
    	//can not be reached
    	return new BigInteger[0];
    	
    }
    public void shareData(BigInteger[] data) {
    	shareValues(data);
        try {
			model.populateShareMessages();
		} catch (IllegalStateException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public CovidApp(Configuration config) {
        this.config = config;
        modelLock = new ReentrantReadWriteLock();
        resultLock = new ReentrantReadWriteLock();
        initialized = new CountDownLatch(1);
        if (config.host == true) {
            allClientsInitialized = new CountDownLatch(config.participants.length - 1);
        } else {
            allClientsInitialized = new CountDownLatch(1);
        }
        sumComputable = new CountDownLatch(1);
        resultComputable = new CountDownLatch(1);
        resultMessageBuffer = new ResultMessageBuffer(config.participants.length);
        rerunComputation = new CountDownLatch(1);
        rerunTimer = null;
    }

    private void runNetworkListener() throws UnrecoverableKeyException, KeyManagementException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException {
        networkListener = new NetworkListener(this, config);
        new Thread(networkListener).start();
    }

    private void runRestServer() throws UnrecoverableKeyException, KeyManagementException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException {
        RestServer endpoint = new RestServer(this, this.config);
        endpoint.run();
    }
    private void setTimer() {
    	rerunTimer = new Timer();
    	rerunTimer.scheduleAtFixedRate( new TimerTask() {
    		@Override
    		public void run() {
    			rerunComputation.countDown();
    		}
    	}, 
    			config.periodicRebuildSeconds*1000,
    			config.periodicRebuildSeconds*1000);
    }

    private void shareValues(BigInteger[] data) {
        try {
            writeLockModel();
            for (int i = 0; i < model.bins.length; i++) {
                model.bins[i].shareValue(data[i]);
            }
        } finally {
            writeUnlockModel();
        }

    }

    private AppModel generateInitializedModel(){
        AppModel model = new AppModel();
        Participant[] participants = config.participants;
        Bin[] bins = new Bin[config.binNames.length];
		HistogramImporter importer = null;
		try {
			importer = new CSVImporter(config.dataFile, config.binNames.length);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        BigInteger[] data = importer.getCounts();
        System.out.println("Read Data: "+ Arrays.toString(data));
        for (int i = 0; i < bins.length; ++i) {
            bins[i] = new Bin(config.binNames[i]);
            bins[i].initialize(participants.length);
            bins[i].shareValue(data[i]);
        }

        model.toStarting();
        model.toInitialSending(config.studyName, participants, bins);
        return model;
    }

    public String getResultMessage() {
        if (lastUpdate == null || lastResult == null)
            return "NotReadyYet";
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        df.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
        try {
            resultLock.readLock().tryLock(5, TimeUnit.SECONDS);
            String result = "{\"timestamp\": \"" + df.format(lastUpdate) + "\",";
            result += "\"title\": \""+config.studyName +"\",";
            result += "\"data\": [";
            for (int i = 0; i < config.binNames.length; i++) {
            	result += "{";
                result += "\"name\": \""+config.binNames[i] + "\",";
                result += "\"value\": "+lastResult[i];
                if (i < config.binNames.length -1) {
                	result += "},";
                } else {
                	result += "}";
                }
            }
            result += "]";
            return result + "}";
        } catch (InterruptedException e) {
            System.err.println("Locking of ReadLock resultLock timed out! " + e);
            System.exit(-1);
        } finally {
            resultLock.readLock().unlock();
        }
        // This can not be reached!
        return "";
    }

    public void sendAck() throws UnknownHostException, IllegalArgumentException, IOException {
        try {
            modelLock.readLock().tryLock(5, TimeUnit.SECONDS);
            networkSender.sendAckInitMessage((CovidParticipant) model.getParticipantFromId(0));
        } catch (InterruptedException e) {
            System.err.println("Locking of ReadLock modelLock timed out! " + e);
            System.exit(-1);
        } finally {
            modelLock.readLock().unlock();
        }
    }

    public void initializeHostModel() {
        try {
            modelLock.writeLock().tryLock(5, TimeUnit.SECONDS);
            model = generateInitializedModel();
            ownId = model.ownId;
        } catch (InterruptedException e) {
            System.err.println("Locking of WriteLock modelLock timed out! " + e);
            System.exit(-1);
        } finally {
            modelLock.writeLock().unlock();
        }
    }

    public void initializeNetworkSender() {
        try {
            modelLock.readLock().tryLock(5, TimeUnit.SECONDS);
            CovidParticipant selfParticipant = (CovidParticipant) model.getParticipantFromId(ownId);
            networkSender = new NetworkSender(this, config, selfParticipant);
        } catch (UnrecoverableKeyException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException
                | CertificateException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            modelLock.readLock().unlock();
        }
    }

    public void initializeClientModel() {
        try {
            modelLock.writeLock().tryLock(5, TimeUnit.SECONDS);
            model = new AppModel();
            model.toParticipating();
        } catch (InterruptedException e) {
            System.err.println("Locking of WriteLock modelLock timed out! " + e);
            System.exit(-1);
        } finally {
            modelLock.writeLock().unlock();
        }
    }

    public void sendMessages(MessageType type) {
        try {
            modelLock.readLock().tryLock(5, TimeUnit.SECONDS);
            for (int i = 0; i < model.numParticipants; i++) {
                if (i != ownId) {
                    String msgString = null;
                    if (type != MessageType.DONE_INIT) {
                        Message msg = model.getUnsentMessageFor(i);
                        msgString = Message.serializeMessage(msg);
                    }
                    CovidParticipant recipient = (CovidParticipant) model.getParticipantFromId(i);
                    switch (type) {
                    case INITIAL:
                        networkSender.sendInitialMessage(recipient, msgString);
                        model.markMessageSent(i);
                        break;
                    case SHARE:
                        networkSender.sendShareMessage(recipient, msgString);
                        model.markMessageSent(i);
                        break;
                    case RESULT:
                        networkSender.sendResultMessage(recipient, msgString);
                        model.markMessageSent(i);
                        break;
                    case DONE_INIT:
                        networkSender.sendDoneInitMessage(recipient);
                        break;
                    case RECALC:
                    	networkSender.sendRecalculateMessage(recipient);
                    	break;
                    }
                }
            }
        } catch (IllegalArgumentException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            modelLock.readLock().unlock();
        }

    }

    private void transferMessageBufferToBins() {
        try {
            modelLock.writeLock().tryLock(5, TimeUnit.SECONDS);
            for (int i = 0; i < resultMessageBuffer.messages.length; i++) {
                if (i != ownId) {
                    model.setShareFromContinuousMessage(resultMessageBuffer.messages[i],
                            resultMessageBuffer.participants[i], true);
                }
            }
            resultMessageBuffer.clear();

        } catch (InterruptedException e) {
            System.err.println("Locking of WriteLock modelLock timed out! " + e);
            System.exit(-1);
        } catch (IllegalStateException | IOException | IllegalArgumentException | ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            modelLock.writeLock().unlock();
        }

    }

    public void resetModel() {
        try {
            modelLock.writeLock().tryLock(5, TimeUnit.SECONDS);
            model.clearModel();
        } catch (InterruptedException e) {
            System.err.println("Locking of WriteLock modelLock timed out! " + e);
            System.exit(-1);
        } finally {
            modelLock.writeLock().unlock();
        }
    }

    public BigInteger[] getResult() {
        try {
            modelLock.writeLock().tryLock(5, TimeUnit.SECONDS);
            BinResult[] binResults = model.getAllResults();
            BigInteger[] result = new BigInteger[binResults.length];
            for (int i = 0; i < result.length; i++) {
                result[i] = binResults[i].value;
            }
            return result;
        } catch (InterruptedException e) {
            System.err.println("Locking of WriteLock modelLock timed out! " + e);
            System.exit(-1);
        } finally {
            modelLock.writeLock().unlock();
        }
        // This gets never executed
        return new BigInteger[0];
    }

    public void setNewResult(BigInteger[] result) {
        try {
            resultLock.writeLock().tryLock(5, TimeUnit.SECONDS);
            lastResult = result;
            lastUpdate = new Date();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            resultLock.writeLock().unlock();
        }
    }

    private enum MessageType {
        INITIAL, DONE_INIT, SHARE, RESULT, RECALC
    }

}

class ResultMessageBuffer {
    Message[] messages;
    Participant[] participants;

    public ResultMessageBuffer(int numParticipants) {
        messages = new Message[numParticipants];
        participants = new Participant[numParticipants];
    }

    public void clear() {
        for (Message m : messages) {
            m = null;
        }
        for (Participant p : participants) {
            p = null;
        }
    }

    public void add(Message msg, Participant p, int id) {
        messages[id] = msg;
        participants[id] = p;
    }

    public boolean isComputable() {
        int fullmsg = 0;
        int fullpart = 0;
        for (Message m : messages) {
            if (m != null)
                fullmsg++;
        }
        for (Participant p : participants) {
            if (p != null)
                fullpart++;
        }
        if (fullmsg == messages.length - 1 && fullmsg == fullpart)
            return true;
        return false;
    }
}

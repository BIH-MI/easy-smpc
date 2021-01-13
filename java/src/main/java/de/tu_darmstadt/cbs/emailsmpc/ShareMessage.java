package de.tu_darmstadt.cbs.emailsmpc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

/**
 * Message for shares
 * @author Tobias Kussel
 */
public class ShareMessage implements Serializable {
    
    /**  SVUID. */
    private static final long serialVersionUID = 8085434766713123559L;
    
    /**
     * Decode and verify.
     *
     * @param msg the msg
     * @param sender the sender
     * @param model the model
     * @return the share message
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ClassNotFoundException the class not found exception
     */
    public static ShareMessage decodeAndVerify(String msg, Participant sender, AppModel model)
            throws IOException, ClassNotFoundException {
        ShareMessage sm = decodeMessage(msg);
        if (verify(sm, sender, model))
            return sm;
        else
            throw new IllegalArgumentException("Message invalid");
    }
    
    /**
     * Decode message.
     *
     * @param msg the msg
     * @return the share message
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws IllegalArgumentException the illegal argument exception
     * @throws ClassNotFoundException the class not found exception
     */
    public static ShareMessage decodeMessage(String msg)
            throws IOException, IllegalArgumentException, ClassNotFoundException {
        Decoder decoder = Base64.getDecoder();
        ByteArrayInputStream stream = new ByteArrayInputStream(decoder.decode(msg));
        ObjectInputStream ois = new ObjectInputStream(stream);
        Object o = ois.readObject();
        if (!(o instanceof ShareMessage))
            throw new IllegalArgumentException("Message invalid");
        return (ShareMessage) o;
    }
    
    /**
     * Verify.
     *
     * @param msg the msg
     * @param sender the sender
     * @param model the model
     * @return true, if successful
     */
    public static boolean verify(ShareMessage msg, Participant sender, AppModel model) {
        return msg.sender.equals(sender) && msg.recipient.equals(model.participants[model.ownId])
                && msg.bins.length == model.bins.length;
    }

    /** The bins. */
    public MessageBin[] bins;

    /** The recipient. */
    public Participant recipient;

    /** The sender. */
    public Participant sender;

    /**
     * Instantiates a new share message.
     *
     * @param model the model
     * @param recipientId the recipient id
     */
    public ShareMessage(AppModel model, int recipientId) {
        this.recipient = model.participants[recipientId];
        this.sender = model.participants[model.ownId];
        this.bins = new MessageBin[model.bins.length];
        for (int i = 0; i < model.bins.length; i++) {
            bins[i] = new MessageBin(model.bins[i], recipientId);
        }
    }

    /**
     * Equals.
     *
     * @param o the o
     * @return true, if successful
     */
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ShareMessage))
            return false;
        ShareMessage msg = (ShareMessage) o;
        if (bins.length != msg.bins.length)
            return false;
        boolean equal = this.sender.equals(msg.sender);
        equal = equal && this.recipient.equals(msg.recipient);
        for (int i = 0; i < bins.length; i++) {
            equal = equal && bins[i].equals(msg.bins[i]);
            if (equal == false) // Short circuit comparisons if false
                return false;
        }
        return equal;
    }

    /**
     * Gets the message.
     *
     * @return the message
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public String getMessage() throws IOException {
        Encoder encoder = Base64.getEncoder();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(stream);
        oos.writeObject(this);
        return encoder.encodeToString(stream.toByteArray());
    }

    /**
     * Hash code.
     *
     * @return the int
     */
    @Override
    public int hashCode() {
        int result = recipient.hashCode();
        result = 31 * result + sender.hashCode();
        for (MessageBin b : bins) {
            result = 31 * result + b.hashCode();
        }
        return result;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        String result = "Recipient: " + recipient.toString() + "\nSender: " + sender.toString();
        result = result + "\nData:\n";
        for (MessageBin b : bins) {
            result = result + b.toString() + "\n";
        }
        return result;
    }
}

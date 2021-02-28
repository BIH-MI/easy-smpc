/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bihealth.mi.easysmpc;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.http.client.utils.URIBuilder;
import org.bihealth.mi.easybus.BusException;
import org.bihealth.mi.easybus.Scope;
import org.bihealth.mi.easysmpc.components.ComponentTextField;
import org.bihealth.mi.easysmpc.components.EntryParticipantSendMail;
import org.bihealth.mi.easysmpc.components.ScrollablePanel;
import org.bihealth.mi.easysmpc.resources.Resources;

import de.tu_darmstadt.cbs.emailsmpc.Message;
import de.tu_darmstadt.cbs.emailsmpc.Participant;
import de.tu_darmstadt.cbs.emailsmpc.Study.StudyState;

/**
 * A perspective
 * 
 * @author Fabian Prasser
 * @author Felix Wirth
 */
public class Perspective2Send extends Perspective implements ChangeListener {
    
    /** Panel for participants */
    private ScrollablePanel    participants;

    /** Text field containing title of study */
    private ComponentTextField title;

    /** Proceed button */
    private JButton            proceed;

    /** Send button manual */
    private JButton            sendAllManual;
    
    /** Send button automatic */
    private JButton            resendAllAutomatic;

    /**
     * Creates the perspective
     * @param app
     */
    protected Perspective2Send(App app) {
        super(app, Resources.getString("PerspectiveSend.send"), 2, true); //$NON-NLS-1$
    }

    /**
     * Creates the perspective
     * @param app
     * @param progress
     */
    protected Perspective2Send(App app, int progress) {
        super(app, Resources.getString("PerspectiveSend.send"), progress, true); //$NON-NLS-1$
    }
    
    /**
     * Creates the perspective
     * @param app
     * @param progress
     */
    protected Perspective2Send(App app, String title , int progress) {
        super(app, title, progress, true); //$NON-NLS-1$
    }
    
    /**
      * Reacts on all changes in any components
      */
     @Override
     public void stateChanged(ChangeEvent e) {
         // Check clickable send all mails button and save button
         boolean messagesUnsent = getApp().getModel().messagesUnsent();
         this.proceed.setEnabled(!messagesUnsent);
         this.sendAllManual.setEnabled(messagesUnsent);
         this.resendAllAutomatic.setEnabled(messagesUnsent && isAutomaticProcessingEnabled());

         // Check buttons clickable
         for (Component c : this.participants.getComponents()) {
                 ((EntryParticipantSendMail) c).setButtonEnabled(isMailButtonClickable(c));
             }
     }
    
    /**
     * Indicates whether the automatic processing enabled
     * 
     * @return enabled
     */
    private boolean isAutomaticProcessingEnabled() {
        // Return if automatic connection is enabled and it is not initial sending of study creator
        return getApp().getModel().connectionIMAPSettings != null &&
               !(getApp().getModelState() == StudyState.INITIAL_SENDING);
    }

    /**
     * Returns the exchange string for the given entry
     * 
     * @param entry
     * @throws IOException
     */
    private String getExchangeString(EntryParticipantSendMail entry) throws IOException {
        int index = Arrays.asList(participants.getComponents()).indexOf(entry);
        return Message.serializeMessage(getApp().getModel().getUnsentMessageFor(index));
    }

    /**
     * Validates each send mail button whether it should be clickable
     */
    private boolean isMailButtonClickable(Component c) {
        int index = Arrays.asList(participants.getComponents()).indexOf(c);
        if (index == getApp().getModel().ownId ||
            getApp().getModel().getUnsentMessageFor(index) == null) {
            return false;
        }
        return true;
    }
    
     /**
      * Returns whether this is the own entry
      * @param entry
      * @return
      */
    private boolean isOwnEntry(Component entry) {
        return Arrays.asList(participants.getComponents()).indexOf(entry) == getApp().getModel().ownId;
    }
    
    /**
     * Returns whether there are unsent message for the entry
     * @param entry
     * @return
     */
   private boolean unsentMessages(Component entry) {
       return getApp().getModel().getUnsentMessageFor(Arrays.asList(participants.getComponents()).indexOf(entry)) != null;                     
   }
     
    /**
     * Proceed action
     */
    protected void actionProceed() {
        getApp().actionFirstSendingDone();
    }
    
     /**
     * Sends an e-mail to the participant entry
     * 
     * @param list
     */
    protected void actionSendMailManual(List<EntryParticipantSendMail> list) {
        try {
            
            // For each entry
            for (EntryParticipantSendMail entry : list) {
                
                // Prepare URI parts
                String subject = String.format(Resources.getString("PerspectiveSend.mailSubject"),
                                               getApp().getModel().name,
                                               getApp().getModel().state == StudyState.SENDING_RESULT ? 2 : 1);
                String exchangeString = Resources.MESSAGE_START_TAG + "\n" + getExchangeString(entry) + "\n" + Resources.MESSAGE_END_TAG;
                exchangeString = exchangeString.replaceAll("(.{" + Resources.MESSAGE_LINE_WIDTH + "})", "$1\n");
                String body = String.format(Resources.getString("PerspectiveSend.mailBody"),
                                            entry.getLeftValue(), // Name of participant
                                            getApp().getModel().state == StudyState.SENDING_RESULT ? 5 : 3, // Step number
                                            exchangeString,
                                            getApp().getModel().participants[getApp().getModel().ownId].name);
                
                // Build URI
                URIBuilder builder = new URIBuilder().setScheme("mailto");
                builder.setPath(entry.getRightValue()) // E-mail address
                       .addParameter("subject", subject)
                       .addParameter("body", body);
                
                // Open email
                Desktop.getDesktop().mail(new URI(builder.toString().replace("+", "%20").replace(":/", ":")));
            }

            // Send a dialog to confirm mail sending
            if (JOptionPane.showConfirmDialog(this.getPanel(), String.format(Resources.getString("PerspectiveSend.confirmSendMailGeneric")), "", JOptionPane.OK_CANCEL_OPTION) == 0) {
                for (EntryParticipantSendMail entry : list) {
                    int index = Arrays.asList(this.participants.getComponents()).indexOf(entry);
                    getApp().actionMarkMessageSent(index);
                }
                // Persist changes
                getApp().actionSave();
            }

        } catch (IOException | URISyntaxException e) {
            JOptionPane.showMessageDialog(this.getPanel(), Resources.getString("PerspectiveSend.mailToError"), Resources.getString("PerspectiveSend.mailToErrorTitle"), JOptionPane.ERROR_MESSAGE);
        }
        this.stateChanged(new ChangeEvent(this));
    }
    
    /**
     * Sends an e-mail to the participant entry automatically
     * 
     * @param list
     */
    protected void actionSendMailAutomatic(List<EntryParticipantSendMail> list) {
        try {
            if (getApp().getEMailBus() == null) {
                throw new BusException("Unable to obtain e-mail bus");
            }
            boolean messageSent = false;
            for (EntryParticipantSendMail entry : list) {
                // Send message
                getApp().getEMailBus()
                        .send(new org.bihealth.mi.easybus.Message(getExchangeString(entry)),
                              new Scope(getApp().getModel().studyUID + getStepIdentifier()),
                              new org.bihealth.mi.easybus.Participant(entry.getLeftValue(),
                                                                      entry.getRightValue()));

                // Mark message sent
                int index = Arrays.asList(this.participants.getComponents()).indexOf(entry);
                getApp().actionMarkMessageSent(index);
                messageSent = true;
            }
            // Persist changes
            if (messageSent) {
                getApp().actionSave();
                }
        } catch (BusException | IOException e) {
            JOptionPane.showMessageDialog(getPanel(),
                                          Resources.getString("PerspectiveSend.sendAutomaticError"),
                                          Resources.getString("PerspectiveSend.sendAutomaticErrorTitle"),
                                          JOptionPane.ERROR_MESSAGE);
        }
        this.stateChanged(new ChangeEvent(this));
    }

    
    /**
     * Returns an identifier for the current step
     * 
     * @return
     */
    protected String getStepIdentifier() {
        return Resources.STEP_1;
    }

    /**
     * List all participants with unsent messages
     * 
     * @return list of participants
     */
    private List<EntryParticipantSendMail> listUnsent() {
        List<EntryParticipantSendMail> list = new ArrayList<>();
        for (Component c : participants.getComponents()) {
            if (!isOwnEntry(c) && unsentMessages(c) ) {
                list.add((EntryParticipantSendMail)  c);                        
            }
        }
        return list;
    }

    /**
     *Creates and adds UI elements
     */
    @Override
    protected void createContents(JPanel panel) {

        // Layout
        panel.setLayout(new BorderLayout());

        // Study title
        JPanel title = new JPanel();
        panel.add(title, BorderLayout.NORTH);
        title.setLayout(new BorderLayout());
        title.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                                                         Resources.getString("PerspectiveCreate.studyTitle"),
                                                         TitledBorder.LEFT,
                                                         TitledBorder.DEFAULT_POSITION));
        this.title = new ComponentTextField(null); // No validation
        this.title.setEnabled(false);
        title.add(this.title, BorderLayout.CENTER);
        
        // Participants
        this.participants = new ScrollablePanel();
        this.participants.setLayout(new BoxLayout(this.participants, BoxLayout.Y_AXIS));
        JScrollPane pane = new JScrollPane(participants, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        pane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                                                                           Resources.getString("PerspectiveSend.participants"),
                                                                           TitledBorder.LEFT,
                                                                           TitledBorder.DEFAULT_POSITION));
        panel.add(pane, BorderLayout.CENTER);    
           
        // Send all e-mails button manually
        JPanel buttonsPane = new JPanel();
        buttonsPane.setLayout(new GridLayout(3, 1));
        sendAllManual = new JButton(Resources.getString("PerspectiveSend.sendAllEmailsButtonManual"));
        sendAllManual.addActionListener(new ActionListener() {            
            @Override
            public void actionPerformed(ActionEvent e) {
                actionSendMailManual(listUnsent());
            }
        });
        buttonsPane.add(sendAllManual, 0, 0);
        // Send all e-mails automatically button
        resendAllAutomatic = new JButton(Resources.getString("PerspectiveSend.sendAllEmailsButtonAutomatic"));
        resendAllAutomatic.addActionListener(new ActionListener() {            
            @Override
            public void actionPerformed(ActionEvent e) {
                actionSendMailAutomatic(listUnsent());
            }
        });
        buttonsPane.add(resendAllAutomatic, 0, 1);
        // proceed button
        proceed = new JButton(Resources.getString("PerspectiveSend.proceed"));
        proceed.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionProceed();
            }
        });
        buttonsPane.add(proceed, 0, 2);
        panel.add(buttonsPane, BorderLayout.SOUTH);
    }

    /**
     * Initialize perspective based on model
     */
    @Override
    protected void initialize() {
        this.title.setText(getApp().getModel().name);
        this.participants.removeAll();
        int i = 0; // Index count for participants to access messages
        for (Participant currentParticipant : getApp().getModel().participants) {
            EntryParticipantSendMail entry = new EntryParticipantSendMail(currentParticipant.name,
                                                                          currentParticipant.emailAddress,
                                                                          i != getApp().getModel().ownId);
            participants.add(entry);
            entry.setButtonListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    List<EntryParticipantSendMail> list = new ArrayList<>();
                    list.add(entry);
                    actionSendMailManual(list);
                }
            });
            i++;
        }
        
        // Send e-mails automatically if enabled 
        if (isAutomaticProcessingEnabled()) {
            actionSendMailAutomatic(listUnsent());
        }
        
        // Update GUI
        this.stateChanged(new ChangeEvent(this));
        getPanel().revalidate();
        getPanel().repaint();    
    }
}

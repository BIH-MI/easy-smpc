package de.tu_darmstadt.cbs.emailsmpc;

public class AppModel {
    public int numParticipants;
    public int ownId;
    public AppState state;
    public Bin[] bins;
    public Participant[] participants;
    public String name;
    public String[] unsentMessages;

    public AppModel() {
        name = null;
        numParticipants = 0;
        ownId = 0;
        state = AppState.NONE;
        bins = null;
        participants = null;
        unsentMessages = null;
    }

    public void initializeStudy(String name, Participant[] participants, Bin[] bins) throws IllegalStateException {
        if (state != AppState.NONE || state != AppState.STARTING)
            throw new IllegalStateException("Unable to initialize study at state" + state);
        this.name = name;
        numParticipants = participants.length;
        for (Bin bin : bins) {
            if (!(bin.isInitialized()))
                throw new IllegalStateException("Unable to initialize bin " + bin.name);
            bin.initialize(numParticipants);
        }
        this.bins = bins;
        this.ownId = 0; // unneeded but for verbosity...
        this.participants = participants;
        if (state == AppState.NONE)
            state = AppState.STARTING;
    }

    /**
     * Legal State Transitions:
     * +-----------------+     +------------------+
     * |    Starting     | <-- |       None       |
     * +-----------------+     +------------------+
     *   |                       |
     *   |                       |
     *   v                       v
     * +-----------------+     +------------------+
     * | Initial_Sending |     |  Participating   |
     * +-----------------+     +------------------+
     *   |                       |
     *   |                       |
     *   |                       v
     *   |                     +------------------+
     *   |                     |  Sending_Share   |
     *   |                     +------------------+
     *   |                       |
     *   |                       |
     *   |                       v
     *   |                     +------------------+
     *   +-------------------> | Recieving_Share  |
     *                         +------------------+
     *                           |
     *                           |
     *                           v
     *                         +------------------+
     *                         |  Sending_Result  |
     *                         +------------------+
     *                           |
     *                           |
     *                           v
     *                         +------------------+
     *                         | Recieving_Result |
     *                         +------------------+
     *                           |
     *                           |
     *                           v
     *                         +------------------+
     *                         |     Finished     |
     *                         +------------------+
     */

    public void advanceState(AppState newState) throws IllegalStateException {
        if (state == AppState.NONE) {
            if (!(newState == AppState.STARTING || newState == AppState.PARTICIPATING))
                throw new IllegalStateException("Illegal state transition from " + state + " to " + newState);
        }
        switch (state) {
        case STARTING:
            if (!(newState == AppState.INITIAL_SENDING))
                throw new IllegalStateException("Illegal state transition from " + state + " to " + newState);
            break;
        case PARTICIPATING:
            if (newState != AppState.SENDING_SHARE)
                throw new IllegalStateException("Illegal state transition from " + state + " to " + newState);
            break;
        case INITIAL_SENDING:
            if (newState != AppState.RECIEVING_SHARE)
                throw new IllegalStateException("Illegal state transition from " + state + " to " + newState);
            break;
        case SENDING_SHARE:
            if (newState != AppState.RECIEVING_SHARE)
                throw new IllegalStateException("Illegal state transition from " + state + " to " + newState);
            break;
        case RECIEVING_SHARE:
            if (newState != AppState.SENDING_RESULT)
                throw new IllegalStateException("Illegal state transition from " + state + " to " + newState);
            break;
        case SENDING_RESULT:
            if (newState != AppState.RECIEVING_RESULT)
                throw new IllegalStateException("Illegal state transition from " + state + " to " + newState);
            break;
        case RECIEVING_RESULT:
            if (newState != AppState.FINISHED)
                throw new IllegalStateException("Illegal state transition from " + state + " to " + newState);
            break;
        case FINISHED:
            throw new IllegalStateException("Illegal state transition: Already finished");
        }

    }

    public Message getInitialMessage(int recipientId) throws IOException{
        InitialMessage data = new InitialMessage(this, recipientId);
        Message msg = new Message();
        Participant recipient = this.participants[recipientId];
        msg.recipientName = recipient.name;
        msg.recipientEmailAddress = recipient.emailAddress;
        msg.data = data.getMessage();
        return msg;
    }

}

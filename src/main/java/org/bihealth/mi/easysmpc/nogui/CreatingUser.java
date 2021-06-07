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
package org.bihealth.mi.easysmpc.nogui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bihealth.mi.easybus.implementations.email.ConnectionIMAPSettings;

import de.tu_darmstadt.cbs.emailsmpc.Bin;
import de.tu_darmstadt.cbs.emailsmpc.Participant;

/**
 * A creator in an EasySMPC process
 * 
 * @author Felix Wirth
 *
 */
public class CreatingUser extends User {
    
    /** All participating users */
    private final List<User> participatingUsers = new ArrayList<>();
    /** Logger */
    Logger logger = LogManager.getLogger(CreatingUser.class);
    
    /**
     * Create a new instance
     * 
     * @param numberParticipants
     * @param numberBins
     * @param connectionIMAPSettings
     * @throws IllegalStateException
     */
    CreatingUser(int numberParticipants,
                 int numberBins,
                 int mailBoxCheckInterval,
                 ConnectionIMAPSettings connectionIMAPSettings) throws IllegalStateException {
        super(mailBoxCheckInterval);

        
        try {          
            // Set model to starting
            getModel().toStarting();
            
            // Init model with generated study name, participants and bins 
            getModel().toInitialSending(generateRandomString(FIXED_LENGTH_STRING),
                                        generateParticpants(numberParticipants, FIXED_LENGTH_STRING),
                                        generateBins(numberBins,numberParticipants, FIXED_LENGTH_STRING, FIXED_LENGTH_BIT_BIGINTEGER), connectionIMAPSettings);
            RecordTimeDifferences.init(getModel(), mailBoxCheckInterval, System.nanoTime());
        } catch (IOException | IllegalStateException e) {
            logger.error("Unable to init logged", new Date(), "Unable to init", ExceptionUtils.getStackTrace(e));
            throw new IllegalStateException("Unable to init study!", e);
        }
        
        // Spawn all participating users
        createParticipants(FIXED_LENGTH_BIT_BIGINTEGER);
        
        // Spawns the common steps in an own thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                proceedCommonProcessSteps();
            }
        }).start();
    }


    /**
     * Create participants
     * 
     * @param lengthBitBigInteger
     */
    private void createParticipants(int lengthBitBigInteger) {
        // Loop over participants
        for(int index = 1; index < getModel().numParticipants; index++) {
            
            // Create user
            participatingUsers.add(new ParticipatingUser(getModel().studyUID,
                                  getModel().participants[index],
                                  index,
                                  getModel().connectionIMAPSettings,
                                  lengthBitBigInteger,
                                  getMailBoxCheckInterval()));
        }
    }


    /**
     * Generate the involved participants
     * 
     * @param numberParticipants
     * @param stringLength length of names and e-mail address parts
     * @return
     */
    private Participant[] generateParticpants(int numberParticipants, int stringLength) {
        // Init result
        Participant[] result = new Participant[numberParticipants];
        
        // Init each participant with a generated name and generated mail address
        for(int index = 0; index < numberParticipants; index++) {
            result[index] = new Participant(generateRandomString(stringLength),
                                            generateRandomString(stringLength) + "@" + generateRandomString(stringLength) + ".de");
        }
        return result;
    }


    /**
     * Generate bins
     * 
     * @param numberBins number of bins
     * @param numberParties number of involved parties/users
     * @param stringLength length of bin name
     * @param bigIntegerBitLength length of generated big integer
     * @return
     */
    protected Bin[] generateBins(int numberBins,
                                 int numberParties,
                                 int stringLength,
                                 int bigIntegerBitLength) {
        // Init result bin array
        Bin[] result = new Bin[numberBins];
        
        // Init each bin and set generated secret value of creating user
        for (int index = 0; index < numberBins; index++) {
            result[index] = new Bin(generateRandomString(stringLength), numberParties);
            result[index].shareValue(generateRandomBigInteger(bigIntegerBitLength));
        }
        
        // Return
        return result;
    }
    
    /**
     * Are all users finished?
     * 
     * @return
     */
    public boolean areAllUsersFinished() {
        // Check for this creating users
        if (!isProcessFinished()) {
            return false;
        }
        
        // Check for all participating users
        for(User user : participatingUsers) {
            if(!user.isProcessFinished()) {
                return false;
            }
        }
        
        // Return all
        return true;
    }
}
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
package de.tu_darmstadt.cbs.app;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import de.tu_darmstadt.cbs.emailsmpc.AppState;

/**
 * A perspective
 * 
 * @author Fabian Prasser
 */

public class PerspectiveStart extends Perspective {

    /**
     * Creates the perspective
     * @param app
     */
    protected PerspectiveStart(App app) {
        super(app, Resources.getString("PerspectiveStart.0")); //$NON-NLS-1$
    }

    /**
     * Creates and adds UI elements
     */
    @Override
    protected void createContents(JPanel panel) {

        // Buttons panel
        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(3, 1));

        // Action 1
        JButton button1 = new JButton(Resources.getString("PerspectiveStart.1")); //$NON-NLS-1$
        buttons.add(button1);
        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SMPCServices.getServicesSMPC().setWorkflowState(AppState.INITIAL_SENDING);
                SMPCServices.getServicesSMPC().commandAndControl();
            }
        });

        // Action 2
        JButton button2 = new JButton(Resources.getString("PerspectiveStart.2")); //$NON-NLS-1$
        buttons.add(button2);
        button2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SMPCServices.getServicesSMPC().setWorkflowState(AppState.PARTICIPATING);
                SMPCServices.getServicesSMPC().commandAndControl();
            }
        });

        // Action 3
        JButton button3 = new JButton(Resources.getString("PerspectiveStart.3")); //$NON-NLS-1$
        buttons.add(button3);
        button3.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int returnFileChooser;
                returnFileChooser = fileChooser.showSaveDialog((Component) e.getSource());
                if (returnFileChooser == JFileChooser.APPROVE_OPTION) {
                    try {
                        SMPCServices.getServicesSMPC().loadFile(fileChooser.getSelectedFile());
                        SMPCServices.getServicesSMPC().commandAndControl();
                    } catch (ClassNotFoundException | IllegalArgumentException | IOException e1) {
                        // TODO Auto-generated catch block
                        JOptionPane.showMessageDialog(null, Resources.getString("PerspectiveStart.errorLoading"));
                    }
                }
            }
        });

        // Add
        panel.setLayout(new GridBagLayout());
        panel.add(buttons, new GridBagConstraints());
    }
}

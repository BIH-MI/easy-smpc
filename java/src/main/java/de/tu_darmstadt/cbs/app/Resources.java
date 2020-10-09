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

import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 * Accessor class for messages used by the UI
 * 
 * @author Fabian Prasser
 */
public class Resources {
    
    /** Count of minimal participants (including study creator) allowed. Set to at least 3 for productive usage*/
    public static int                   MINIMAL_PARTICIPANTSCOUNT         = 1;
    
    /** Row gap*/
    public static int                   ROW_GAP         = 5;
    
    /** Row height*/
    public static final int             ROW_HEIGHT      = 20;
    
    /** Row height for a text area*/
    public static final int             COLOUMNS_TEXTAREA      = 80;
    
    
    /** Maximal x-size of a text field*/
    public static int                   MAX_SIZE_TEXTFIELD_X         = 500;
    
    /** Maximal y-size of a text field*/
    public static int                   MAX_SIZE_TEXTFIELD_Y         = 30;
    
    /** Min x-size of a text area*/
    public static int                   SIZE_TEXTAREA_X         = 400;
    
    /** Min y-size of a text area*/
    public static int                   SIZE_TEXTAREA_Y         = 100;

    
    /** Default column size for a text field*/
    public static int                   DEFAULT_COLUMN_SIZE         = 10;
    
    /** Bundle name */
    private static final String         BUNDLE_NAME     = "de.tu_darmstadt.cbs.app.messages";   //$NON-NLS-1$

    /** Bundle */
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

    /**
     * No instantiation
     */
    private Resources() {
        // Empty by design
    }

    /**
     * Returns a message
     * 
     * @param key
     * @return
     */
    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    /**
     * Icon
     * 
     * @return
     * @throws IOException
     */
    public static Image getIcon() throws IOException {
        InputStream stream = Resources.class.getResourceAsStream("icon.png");
        return ImageIO.read(stream);
    }

    /**
     * Menu item
     * 
     * @return
     * @throws IOException
     */
    public static ImageIcon getMenuItem() throws IOException {
        InputStream stream = Resources.class.getResourceAsStream("icon.png");
        return new ImageIcon(ImageIO.read(stream).getScaledInstance(16, 16, Image.SCALE_DEFAULT));
    }
}

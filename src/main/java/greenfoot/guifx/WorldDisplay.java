/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2017,2018  Poul Henriksen and Michael Kolling
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package greenfoot.guifx;

import bluej.utility.javafx.FXPlatformConsumer;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A class for handling the world part of the main GreenfootStage window.
 */
@OnThread(value = Tag.FXPlatform, ignoreParent = true)
public class WorldDisplay extends StackPane
{
    private final ImageView imageView = new ImageView();
    private final AskPaneFX askPane = new AskPaneFX();
    
    public WorldDisplay()
    {
        // Need a stack pane to be able to provide border around image and Greenfoot.ask() :
        StackPane stackPane = new StackPane(imageView,askPane);
        stackPane.getStyleClass().add("world-display-wrapper");
        // Make StackPane fit exactly around the contained imageView:
        stackPane.setMaxWidth(USE_PREF_SIZE);
        stackPane.setMaxHeight(USE_PREF_SIZE);
        getChildren().addAll(stackPane);
        setMinWidth(200);
        setMinHeight(200);
    }

    /**
     * Sets the world image.  Turns off any greying effect.
     * 
     * Returns true if the world changed size
     */
    public boolean setImage(Image image)
    {
        Image oldImage = imageView.getImage();
        boolean newSize = oldImage == null || image == null ||
                image.getWidth() != oldImage.getWidth() ||
                image.getHeight() != oldImage.getHeight();
        imageView.setImage(image);
        // Now that world is valid again, turn off any greying effect:
        imageView.setEffect(null);
        return newSize;
    }

    /**
     * Greys out the world to indicate it isn't in a valid state.
     * Turned off by next setImage call.
     */
    public void greyOutWorld()
    {
        ColorAdjust grey = new ColorAdjust(0.0, -1.0, -0.1, 0.0);
        GaussianBlur blur = new GaussianBlur();
        blur.setInput(grey);
        imageView.setEffect(blur);
    }

    /**
     * Show the ask panel with the given prompt, and call the given callback once
     * the user gives an answer.  It is safe to call this method repeatedly for the
     * same ask request without disturbing the GUI.
     */
    @OnThread(Tag.FXPlatform)
    public void ensureAsking(String prompt, FXPlatformConsumer<String> withAnswer)
    {
        // Remember, all of this should be fine to call multiple times:
        imageView.setDisable(true);
        greyOutWorld();
        askPane.setVisible(true);
        askPane.setPrompt(prompt);
        askPane.focusTextEntry();
        askPane.setWithAnswer(ans -> {
            // Reverse the above GUI changes then pass it on:
            askPane.setVisible(false);
            imageView.setDisable(false);
            imageView.setEffect(null);
            withAnswer.accept(ans);
            // Put focus back on the world display:
            requestFocus();
        });
    }

    /**
     * Is the ask pane currently showing?
     */
    public boolean isAsking()
    {
        return askPane.isVisible();
    }

    /**
     * Converts a point in the scene to world coordinates.
     */
    public Point2D sceneToWorld(Point2D point2D)
    {
        // We use imageView, not ourselves, because there may be extra margin around the imageView
        // in this StackPane if the user has sized the window to be larger than the world size.
        return imageView.sceneToLocal(point2D);
    }

    /**
     * Converts a point in world coordinates into screen coordinates.
     */
    public Point2D worldToScreen(Point2D point2D)
    {
        // We use imageView, not ourselves, because there may be extra margin around the imageView
        // in this StackPane if the user has sized the window to be larger than the world size.
        return imageView.localToScreen(point2D);
    }

    /**
     * Checks if the given point (in world coordinates) lies inside the world or not.
     */
    public boolean worldContains(Point2D point2D)
    {
        return imageView.contains(point2D);
    }

    /**
     * Is the world greyed out?  (It will be if there has been a call
     * to greyOutWorld(), but not yet followed by setImage)
     */
    public boolean isGreyedOut()
    {
        return imageView.getEffect() != null;
    }

    /**
     * Returns a rendered image which illustrates a snapshot of the world display.
     * @return the rendered image
     */
    public Image getSnapshot()
    {
        return imageView.snapshot(null, null);
    }
    
    /**
     * Get the node containing the actual world image (sans any spacing/border).
     */
    public Node getImageView()
    {
        return imageView;
    }
}

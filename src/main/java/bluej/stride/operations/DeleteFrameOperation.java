/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg
 
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
package bluej.stride.operations;

import bluej.Config;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCursor;
import bluej.stride.generic.InteractionManager;
import bluej.stride.slots.EditableSlot.MenuItemOrder;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class DeleteFrameOperation extends FrameOperation
{

    public DeleteFrameOperation(InteractionManager editor)
    {
        super(editor, "DELETE", Combine.ALL, new KeyCodeCombination(KeyCode.DELETE));
    }

    @Override
    public List<ItemLabel> getLabels()
    {
        return Arrays.asList(l(Config.getString("frame.operation.delete"), MenuItemOrder.DELETE));
    }

    @OnThread(Tag.FXPlatform)
    @Override
    public void enablePreview()
    {
        editor.getSelection().setDeletePreview(true);
    }

    @OnThread(Tag.FXPlatform)
    @Override
    public void disablePreview()
    {
        editor.getSelection().setDeletePreview(false);
    }

    @Override
    @OnThread(Tag.FXPlatform)
    protected void execute(List<Frame> frames)
    {
        int effort = frames.stream().mapToInt(Frame::calculateEffort).sum();
        editor.showUndoDeleteBanner(effort);
        deleteFrames(frames, editor);
    }

    @OnThread(Tag.FXPlatform)
    public static void deleteFrames(List<Frame> frames, InteractionManager editor)
    {
        if (!frames.isEmpty())
        {
            FrameCursor focusAfter = frames.get(0).getCursorBefore();
            frames.forEach(frame -> frame.getParentCanvas().removeBlock(frame));
            // Clear selection first to prevent problem in frame cursor focus looking at selection for menu items:
            editor.getSelection().clear();
            Objects.requireNonNull(focusAfter).requestFocus();
        }
        else
        {
            editor.getSelection().clear();
        }
    }

    @Override
    public boolean onlyOnContextMenu()
    {
        return true;
    }
}
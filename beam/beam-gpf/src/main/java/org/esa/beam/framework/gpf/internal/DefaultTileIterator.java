/*
 * $Id: DefaultTileIterator.java,v 1.1 2009-04-28 14:37:14 lveci Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.gpf.internal;

import org.esa.beam.framework.gpf.Tile;

import java.awt.Rectangle;
import java.util.Iterator;
import java.util.NoSuchElementException;


public class DefaultTileIterator implements Iterator<Tile.Pos> {
    private final int x1;
    private final int y1;
    private final int x2;
    private final int y2;
    private int x;
    private int y;
    private boolean done;

    public DefaultTileIterator(Rectangle rectangle) {
        x1 = rectangle.x;
        y1 = rectangle.y;
        x2 = x1 + rectangle.width - 1;
        y2 = y1 + rectangle.height - 1;
        x = x1;
        y = y1;
        done = x > x2 && y > y2;
    }

    @Override
    public final boolean hasNext() {
        return !done;
    }

    @Override
    public final Tile.Pos next() {
        if (done) {
            throw new NoSuchElementException();
        }
        Tile.Pos p = new Tile.Pos(x, y);
        x++;
        if (x > x2) {
            x = x1;
            y++;
            if (y > y2) {
                y = y1;
                done = true;
            }
        }
        return p;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}

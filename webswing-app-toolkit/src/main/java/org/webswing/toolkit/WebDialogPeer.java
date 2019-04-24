package org.webswing.toolkit;

import org.webswing.toolkit.util.Services;
import org.webswing.toolkit.util.Util;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import java.awt.Dialog;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.awt.peer.DialogPeer;
import java.util.List;


public class WebDialogPeer extends WebWindowPeer implements DialogPeer {

    public WebDialogPeer(Dialog t) {
        super(t);
    }

    public void blockWindows(List<Window> windows) {
    }

   
    public void flashWindow()
    {
        if (target instanceof JDialog) {
            if (((JDialog) target).isUndecorated()) {
                return;
            }
        }
        new Thread("Paint_flash_Thread")
        {

            public void run()
            {
                // 两次灰化-》亮化闪烁
                for(int i=0; i<4; i++)
                {
                    paintWindowDecoration((i % 2) == 1);
                    try
                    {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private void paintWindowDecoration(final boolean active)
    {
        final int w, h;
        if (Util.isDD())
        {
            w = webImage.getWidth(null);
            h = webImage.getHeight(null);
            windowDecorationImage = Services.getDirectDrawService().createImage(w, h);
        }
        else
        {
            w = image.getWidth();
            h = image.getHeight();
            windowDecorationImage = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
        }
        SwingUtilities.invokeLater(new Runnable()
        {

            @Override
            public void run()
            {
                Graphics g = windowDecorationImage.getGraphics();
                Services.getImageService().getWindowDecorationTheme().paintWindowDecoration(g, target, w, h, !active);
                g.dispose();
                Util.getWebToolkit().getPaintDispatcher().notifyWindowAreaRepainted(getGuid(), new Rectangle(0, 0, w, h));
            }
        });
    }

}

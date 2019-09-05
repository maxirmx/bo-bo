package org.webswing.demo.html;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.webswing.toolkit.api.WebswingUtil;
import org.webswing.toolkit.api.action.WebActionEvent;
import org.webswing.toolkit.api.action.WebWindowActionListener;
import org.webswing.toolkit.api.component.HtmlPanel;

import com.sun.swingset3.DemoProperties;

@DemoProperties(value = "HtmlElement", category = "Webswing", description = "Demonstrates html element inside Webswing app", sourceFiles = { "org/webswing/demo/html/HtmlElementDemo.java" })
public class HtmlElementDemo extends JPanel {
	private static final long serialVersionUID = 8550928872207603286L;

	public HtmlElementDemo() {
		if (!WebswingUtil.isWebswing()) {
			add(new JLabel("HtmlElement can only be used when swing is running inside Webswing."));
			return;
		}
		
		setLayout(new BorderLayout());
		
		HtmlPanel htmlPanel = WebswingUtil.getWebswingApi().createHtmlPanel();
		htmlPanel.setName("test123");
		htmlPanel.add(new JLabel("Please enable Compositing Window Manager in application config to see this demo."));
		add(htmlPanel, BorderLayout.CENTER);
		
		htmlPanel.addWebWindowActionListener(new WebWindowActionListener() {
			@Override
			public void actionPerformed(WebActionEvent actionEvent) {
				switch (actionEvent.getActionName()) {
					case "openConfirmDialog": {
						SwingUtilities.invokeLater(() -> {
							int result = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(HtmlElementDemo.this), "Are you sure?");
							if (result == JOptionPane.YES_OPTION) {
								htmlPanel.performWebAction("confirmDialogResult", "yes", null);
							} else if (result == JOptionPane.NO_OPTION) {
								htmlPanel.performWebAction("confirmDialogResult", "no", null);
							}
						});
					}
				}
			}
			
			@Override
			public void windowInitialized() {
				System.out.println("HtmlPanel initialized!");
			}
		});
	}

	public static void main(String[] args) {
		final JFrame f = new JFrame("Html Element Example");
		f.getContentPane().add(new HtmlElementDemo());
		f.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		f.pack();
		f.setVisible(true);
	}
	
}

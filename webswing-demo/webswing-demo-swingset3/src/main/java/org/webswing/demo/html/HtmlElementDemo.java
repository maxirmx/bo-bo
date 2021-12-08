package org.webswing.demo.html;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BoxLayout;
import javax.swing.JButton;
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

	private HtmlPanel htmlPanel;
	private HtmlPanel htmlIframePanel;
	private FocusListener buttonFocusListener = new FocusListener() {
		@Override
		public void focusLost(FocusEvent e) {
			if (e.getComponent() instanceof JButton) {
				e.getComponent().setBackground(null);
			}
		}
		
		@Override
		public void focusGained(FocusEvent e) {
			if (e.getComponent() instanceof JButton) {
				e.getComponent().setBackground(Color.yellow);
			}
		}
	};
	
	public HtmlElementDemo() {
		if (!WebswingUtil.isWebswing()) {
			add(new JLabel("HtmlElement can only be used when swing is running inside Webswing."));
			return;
		}
		
		JPanel contentPanel = new JPanel();
		BoxLayout boxLayout = new BoxLayout(contentPanel, BoxLayout.Y_AXIS);
		contentPanel.setLayout(boxLayout);
		add(contentPanel, BorderLayout.CENTER);
		
		JButton btn1 = new JButton("Button 1");
		btn1.setFocusPainted(false);
		btn1.addFocusListener(buttonFocusListener);
		JButton btn2 = new JButton("Button 2");
		btn2.setFocusPainted(false);
		btn2.addFocusListener(buttonFocusListener);
		JButton btn3 = new JButton("Button 3");
		btn3.setFocusPainted(false);
		btn3.addFocusListener(buttonFocusListener);
		
		htmlPanel = WebswingUtil.getWebswingApi().createHtmlPanel();
		htmlPanel.setName("test123");
		htmlPanel.setPreferredSize(new Dimension(300, 200));
		if (!WebswingUtil.getWebswingApi().isCompositingWindowManager()) {
			htmlPanel.add(new JLabel("Please enable Compositing Window Manager in application config to see this demo."));
		}
		
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
		
		htmlIframePanel = WebswingUtil.getWebswingApi().createHtmlPanel();
		htmlIframePanel.setName("testIframe");
		htmlIframePanel.setPreferredSize(new Dimension(300, 200));
		
		contentPanel.add(btn1);
		contentPanel.add(htmlPanel);
		contentPanel.add(btn2);
		contentPanel.add(htmlIframePanel);
		contentPanel.add(btn3);
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

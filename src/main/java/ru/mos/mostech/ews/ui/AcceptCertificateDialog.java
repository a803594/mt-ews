/*
DIT
 */
package ru.mos.mostech.ews.ui;

import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.BundleMessage;
import ru.mos.mostech.ews.http.MosTechEwsX509TrustManager;
import ru.mos.mostech.ews.ui.tray.MosTechEwsTray;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.Date;

/**
 * Accept certificate dialog
 */

@Slf4j
public class AcceptCertificateDialog extends JDialog {
    protected boolean accepted;

    /**
     * Accept status.
     *
     * @return true if user accepted certificate
     */
    public boolean isAccepted() {
        return accepted;
    }

    /**
     * Add a new JLabel to panel with <b>label</b>: value text.
     *
     * @param panel certificate details panel
     * @param label certificate attribute label
     * @param value certificate attribute value
     */
    protected void addFieldValue(JPanel panel, String label, String value) {
        JPanel fieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fieldPanel.add(new JLabel("<html><b>" + label + ":</b></html>"));
        fieldPanel.add(new JLabel(value));
        panel.add(fieldPanel);
    }

    /**
     * Accept certificate dialog.
     *
     * @param certificate certificate sent by server
     */
    public AcceptCertificateDialog(X509Certificate certificate) {
        setAlwaysOnTop(true);
        String sha1Hash = MosTechEwsX509TrustManager.getFormattedHash(certificate);
        DateFormat formatter = DateFormat.getDateInstance(DateFormat.MEDIUM);

        setTitle(BundleMessage.format("UI_ACCEPT_CERTIFICATE"));
        try {
            setIconImages(MosTechEwsTray.getFrameIcons());
        } catch (NoSuchMethodError error) {
            MosTechEwsTray.debug(new BundleMessage("LOG_UNABLE_TO_SET_ICON_IMAGE"));
        }

        JPanel subjectPanel = new JPanel();
        subjectPanel.setLayout(new BoxLayout(subjectPanel, BoxLayout.Y_AXIS));
        subjectPanel.setBorder(BorderFactory.createTitledBorder(BundleMessage.format("UI_SERVER_CERTIFICATE")));
        addFieldValue(subjectPanel, BundleMessage.format("UI_ISSUED_TO"), MosTechEwsX509TrustManager.getRDN(certificate.getSubjectX500Principal()));
        addFieldValue(subjectPanel, BundleMessage.format("UI_ISSUED_BY"), MosTechEwsX509TrustManager.getRDN(certificate.getIssuerX500Principal()));
        Date now = new Date();
        String notBefore = formatter.format(certificate.getNotBefore());
        if (now.before(certificate.getNotBefore())) {
            notBefore = "<html><font color=\"#FF0000\">" + notBefore + "</font></html>";
        }
        addFieldValue(subjectPanel, BundleMessage.format("UI_VALID_FROM"), notBefore);
        String notAfter = formatter.format(certificate.getNotAfter());
        if (now.after(certificate.getNotAfter())) {
            notAfter = "<html><font color=\"#FF0000\">" + notAfter + "</font></html>";
        }
        addFieldValue(subjectPanel, BundleMessage.format("UI_VALID_UNTIL"), notAfter);
        addFieldValue(subjectPanel, BundleMessage.format("UI_SERIAL"), MosTechEwsX509TrustManager.getFormattedSerial(certificate));
        addFieldValue(subjectPanel, BundleMessage.format("UI_FINGERPRINT"), sha1Hash);

        JPanel warningPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel imageLabel = new JLabel();
        imageLabel.setIcon(UIManager.getIcon("OptionPane.warningIcon"));
        imageLabel.setText(BundleMessage.format("UI_UNTRUSTED_CERTIFICATE_HTML"));
        warningPanel.add(imageLabel);
        add(warningPanel, BorderLayout.NORTH);
        add(subjectPanel, BorderLayout.CENTER);
        add(getButtonPanel(), BorderLayout.SOUTH);
        setModal(true);

        pack();
        // center frame
        setLocation(getToolkit().getScreenSize().width / 2 -
                getSize().width / 2,
                getToolkit().getScreenSize().height / 2 -
                        getSize().height / 2);
	setAlwaysOnTop(true);
        setVisible(true);
    }

    protected JPanel getButtonPanel() {
        JPanel buttonPanel = new JPanel();
        JButton accept = new JButton(BundleMessage.format("UI_BUTTON_ACCEPT"));
        JButton deny = new JButton(BundleMessage.format("UI_BUTTON_DENY"));
        accept.addActionListener(evt -> {
            accepted = true;
            setVisible(false);
        });
        deny.addActionListener(evt -> {
            accepted = false;
            setVisible(false);
        });

        buttonPanel.add(accept);
        buttonPanel.add(deny);
        return buttonPanel;
    }


    /**
     * Display certificate accept dialog and get user answer.
     *
     * @param certificate certificate sent by server
     * @return true if user accepted certificate
     */
    public static boolean isCertificateTrusted(final X509Certificate certificate) {
        final boolean[] answer = new boolean[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                AcceptCertificateDialog certificateDialog = new AcceptCertificateDialog(certificate);
                answer[0] = certificateDialog.isAccepted();
            });
        } catch (InterruptedException ie) {
            MosTechEwsTray.error(new BundleMessage("UI_ERROR_WAITING_FOR_CERTIFICATE_CHECK"), ie);
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException ite) {
            MosTechEwsTray.error(new BundleMessage("UI_ERROR_WAITING_FOR_CERTIFICATE_CHECK"), ite);
        }

        return answer[0];
    }
}

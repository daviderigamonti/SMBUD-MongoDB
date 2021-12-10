package com;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.mongodb.client.model.Sorts;
import org.bson.json.JsonObject;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.Dimension;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class QrGUI extends JFrame {
    private JPanel mainPanel;
    private JTabbedPane menu;
    private JPanel createPanel;
    private JPanel insertPanel;
    private JTextField ssn_text;
    private JButton generateButton;
    private JComboBox choose_type;
    private JLabel error_text;
    private JTextField path;
    private JButton choose_file;
    private JLabel image_qr;
    private JLabel name_surname;
    private JLabel error_msg_file;
    private Webcam webcam;

    public QrGUI(String title){
        super(title);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setContentPane(mainPanel);
        this.pack();


        choose_type.addItem("1 Dose");
        choose_type.addItem("2 Dose");
        choose_type.addItem("3 Dose");
        choose_type.addItem("Negative Test");
        choose_type.addItem("Healed Test");
        choose_type.setSelectedIndex(0);

        choose_file.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                error_msg_file.setText("");
                name_surname.setText("");
                image_qr.setIcon(null);
                image_qr.setBorder(null);
                JFileChooser fileChooser = new JFileChooser();
                int n = fileChooser.showOpenDialog(QrGUI.this);
                if(n == JFileChooser.APPROVE_OPTION){
                    File f = fileChooser.getSelectedFile();
                    System.out.println(f.getPath()+" "+f.getName());
                    BufferedImage myPicture = null;
                    try {
                        myPicture = ImageIO.read(f);
                        if(myPicture!=null) {
                            error_msg_file.setForeground(Color.BLACK);
                            error_msg_file.setText("");
                            String charset = "UTF-8";
                            Map<EncodeHintType, ErrorCorrectionLevel> hashMap
                                    = new HashMap<EncodeHintType,
                                    ErrorCorrectionLevel>();
                            hashMap.put(EncodeHintType.ERROR_CORRECTION,
                                    ErrorCorrectionLevel.L);
                            String json_result = MyQr.getIstance().readQR(myPicture, charset, hashMap);
                            byte[] decodedBytes = Base64.getDecoder().decode(json_result);
                            System.out.println("decodedBytes " + new String(decodedBytes));
                            Border border = null;
                            String output = MyDb.getIstance().checkCertificate(new String(decodedBytes));
                            String[] tokens = output.split(";");
                            switch (tokens[0]){
                                case "0":
                                    error_msg_file.setForeground(Color.RED);
                                    error_msg_file.setText(tokens[1]);
                                    break;
                                case "1":
                                    border = BorderFactory.createLineBorder(Color.RED, 2);
                                    name_surname.setText(tokens[1]);
                                    image_qr.setIcon((new ImageIcon(myPicture.getScaledInstance(200, 200, Image.SCALE_FAST))));
                                    image_qr.setBorder(border);
                                    break;
                                case "2":
                                    border = BorderFactory.createLineBorder(Color.GREEN, 2);
                                    name_surname.setText("<html>Name and surname: " + tokens[1] + " " + tokens[2]+"<br/>"+"Birthdate: "+ tokens[3]+"</html>");
                                    image_qr.setIcon((new ImageIcon(myPicture.getScaledInstance(200, 200, Image.SCALE_FAST))));
                                    image_qr.setBorder(border);
                                    break;
                                default:
                                    // code block
                            }
                        }
                        else
                        {
                            error_msg_file.setForeground(Color.RED);
                            error_msg_file.setText("Insert an image!");
                        }
                    } catch (IOException | NotFoundException ex) {
                        ex.printStackTrace();
                    }
                }

            }
        });

        generateButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ssn_text.setBorder(null);
                error_text.setText("");
                String ssn = ssn_text.getText();
                boolean isValid = ssn.length() == 11 &&
                        ssn.charAt(0) <= '9' && ssn.charAt(0) >= '0' &&
                        Character.isDigit(ssn.charAt(1)) && Character.isDigit(ssn.charAt(2)) &&
                        ssn.charAt(3) == '-' && Character.isDigit(ssn.charAt(4)) &&
                        Character.isDigit(ssn.charAt(5)) && ssn.charAt(6) == '-' &&
                        Character.isDigit(ssn.charAt(7)) && Character.isDigit(ssn.charAt(8)) &&
                        Character.isDigit(ssn.charAt(9)) && Character.isDigit(ssn.charAt(10));
                if(ssn.equals("") || !isValid)
                {
                    Border border = BorderFactory.createLineBorder(Color.RED, 1);
                    ssn_text.setBorder(border);
                    error_text.setForeground(Color.RED);
                    error_text.setText("Invalid ssn format!");
                }
                else
                {
                    ssn_text.setBorder(null);
                    String result = MyDb.getIstance().checkSsn(ssn,choose_type.getSelectedItem().toString());
                    if(result == null)
                    {
                        error_text.setForeground(Color.RED);
                        error_text.setText("The certificate doesn't exist!");
                    }
                    else
                    {
                        String path = "qr_code.png";
                        String charset = "UTF-8";

                        Map<EncodeHintType, ErrorCorrectionLevel> hashMap
                              = new HashMap<EncodeHintType,
                                                               ErrorCorrectionLevel>();

                          hashMap.put(EncodeHintType.ERROR_CORRECTION,
                                  ErrorCorrectionLevel.L);

                        try {
                            MyQr.getIstance().createQR(result, path, charset, hashMap, 200, 200);
                        } catch (WriterException ex) {
                            ex.printStackTrace();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }

                        error_text.setForeground(Color.GREEN);
                        error_text.setText("Created the qr_code. Check your folder!");
                    }

                }
            }
        });
    }



    public static void main(String[] args){
        JFrame frame = new QrGUI("QR READER");
        frame.setVisible(true);


    }
}

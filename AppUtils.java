package local.pc.app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AppUtils {
    public static boolean keepGoing = true;
    public static boolean key1 = false;
    public static boolean key2 = false;
    public static boolean key3 = false;

    public static void keepUntil(LocalTime endDate) throws InterruptedException {
        String until = getKeepRunningUntil();
        if (until != null) {
            System.out.println("Already keeping until " + until);
        } else {
            setKeepRunningUntil(endDate);
            System.out.println("Keeping until " + endDate);
            while (LocalTime.now().isBefore(endDate) && keepGoing) {
                simulateEvent();
                Thread.sleep(120000);
            }
            setKeepRunningUntil(null);
        }
    }

    public static void setKeepRunningUntil(LocalTime value) {
        try {
            String fileName = "C:\\ice\\poc\\AppLocal\\src\\local\\pc\\app\\keep.txt";
            File file = new File(fileName);
            if (value != null) {
                if (!file.exists()) {
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
                        writer.append(value.toString());
                    }
//                    file.createNewFile();
                }
            } else if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static String getKeepRunningUntil() {
        try {
            File file = new File("C:\\ice\\poc\\AppLocal\\src\\local\\pc\\app\\keep.txt");
            if (file.exists()) {
                try (InputStream inputStream = new FileInputStream(file)) {
                    return new String(inputStream.readAllBytes());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void simulateEvent() {
        try {
            Robot robot = new Robot();

            // Simulate a mouse click
            robot.mouseRelease(InputEvent.BUTTON1_MASK);

            // Simulate a key press
            robot.keyRelease(KeyEvent.VK_F15);

        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public static void lockWorkstation() throws IOException {
        Runtime.getRuntime().exec("rundll32.exe user32.dll,LockWorkStation");
    }

    public static void showBlankScreens(LocalTime endDate, boolean lockOnTimeout) throws AWTException {
        Robot robot = new Robot();

        simulateKeys(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_WINDOWS, KeyEvent.VK_LEFT);

        SwingUtilities.invokeLater(() -> {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

            GraphicsDevice[] gs = ge.getScreenDevices();
            List<JFrame> frames = new ArrayList<>();
            long startTime = System.currentTimeMillis();

            // Ensure focus and stay on top even if other windows try to take focus
            Thread thread = new Thread(() -> {
                while (Thread.currentThread().isInterrupted()) {
                    try {
                        if (LocalTime.now().isAfter(endDate)) {
                            forceClose(frames, startTime, Thread.currentThread(), lockOnTimeout);
                            break;
                        } else {
                            Thread.sleep(200); // Adjust interval as needed
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    EventQueue.invokeLater(() -> {
                        for (int i = frames.size() - 1; i >= 0; i--) {
                            JFrame frame = frames.get(i);
                            frame.toFront();
                            frame.requestFocus();
                        }
                    });
                }
            });

            for (GraphicsDevice gd : gs) {
                JFrame frame = new JFrame();
                frames.add(frame);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setUndecorated(true);
                frame.getContentPane().setBackground(Color.BLACK);

                // Get the bounds of the current screen
                Rectangle bounds = gd.getDefaultConfiguration().getBounds();

                // Set the frame's size and location to match the screen
                frame.setSize(bounds.width, bounds.height);
                frame.setLocation(bounds.x, bounds.y);

                // Make the frame always stay on top
                frame.setAlwaysOnTop(true);

                // Add KeyListener to handle ESC key press
                frame.addKeyListener(new java.awt.event.KeyAdapter() {
                    public void keyPressed(KeyEvent evt) {
//                        System.out.println("Key Pressed: " + evt.getKeyCode());
                        if (!key2 && !key3 && evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
                            key1 = true;
                        } else if (key1 && !key3 && evt.getKeyCode() == KeyEvent.VK_BACK_QUOTE) {
                            key2 = true;
                        } else if (key1 && key2 && evt.getKeyCode() == KeyEvent.VK_ENTER) {
                            key3 = true;
                        }
                        if (key1 && key2 && key3) {
                            forceClose(frames, startTime, thread, false);
//                            frame.dispose(); // Close the frame
                        }
                    }

                    /**
                     * Invoked when a key has been released.
                     */
                    public void keyReleased(KeyEvent evt) {
                        key1 = false;
                        key2 = false;
                        key3 = false;
                    }
                });
                // Create a blank cursor image
                BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                        cursorImg, new Point(0, 0), "blank cursor");

                // Add MouseMotionListener to handle mouse movements
                frame.addMouseMotionListener(new MouseMotionListener() {
                    @Override
                    public void mouseMoved(MouseEvent e) {
                        frame.setCursor(blankCursor); // Hide cursor
                    }

                    @Override
                    public void mouseDragged(MouseEvent e) {
                        // No action needed for dragging
                    }
                });
            }

            frames.forEach(frame -> frame.setVisible(true));

            System.out.println("Started " + LocalTime.now());
            thread.start();
        });
    }

    private static void forceClose(List<JFrame> frames, long startTime, Thread thread, boolean lock) {
        try {
            keepGoing = false;
            if (lock) {
                AppUtils.lockWorkstation();
            }
            Thread.sleep(1000);
            Iterator<JFrame> iter = frames.iterator();
            while (iter.hasNext()) {
                iter.next().dispose();
                iter.remove();
            }
            frames.clear();
            frames = null;
            Thread.sleep(100);
            thread.interrupt();
            thread.stop();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("Stopped " + LocalTime.now());
            System.out.println("Runtime = " + convertMillisecondsToHumanReadable(System.currentTimeMillis() - startTime));
            System.exit(0);
        }
    }

    public static String convertMillisecondsToHumanReadable(long milliseconds) {
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds);
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static void simulateKeys(Robot robot, int... keyEvents) {
        // Press keys
        for (int keyEvent : keyEvents) {
            robot.keyPress(keyEvent);
        }
        // Release keys
        for (int i = keyEvents.length - 1; i >= 0; i--) {
            robot.keyRelease(keyEvents[i]);
        }
    }
}

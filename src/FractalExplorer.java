import javax.imageio.ImageIO;
import javax.management.remote.JMXConnectorFactory;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.BorderLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class FractalExplorer{
    private int size;
    private JImageDisplay display;
    private FractalGenerator fractal;
    private Rectangle2D.Double range;
    private int rowsRemaining = 0;
    private JButton resetButton;
    private JButton saveButton;
    private JComboBox<FractalGenerator> fractalList;

    FractalExplorer(int size){
        this.size = size;
        this.range = new Rectangle2D.Double();
        this.fractal = new Mandelbrot();

        fractal.getInitialRange(range);
        display = new JImageDisplay(size, size);
    }

    private void createAndShowGUI(){
        JFrame frame = new JFrame("Fractal Explorer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel northPanel = new JPanel();
        JPanel southPanel = new JPanel();

        JLabel label = new JLabel("Fractal:");

        ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox box = (JComboBox) e.getSource();
                FractalGenerator item = (FractalGenerator) box.getSelectedItem();
                fractal = item;
                fractal.getInitialRange(range);
                drawFractal();
            }
        };

        fractalList = new JComboBox();
        FractalGenerator mandelbrot = new Mandelbrot();
        FractalGenerator tricorn = new Tricorn();
        FractalGenerator burningShip = new BurningShip();
        fractalList.addItem(mandelbrot);
        fractalList.addItem(tricorn);
        fractalList.addItem(burningShip);
        fractalList.setSelectedIndex(0);
        fractalList.addActionListener(actionListener);

        northPanel.add(label);
        northPanel.add(fractalList);
        frame.add(northPanel, BorderLayout.NORTH);


        MouseListener mouseListener = new MouseListener();
        display.addMouseListener(mouseListener);
        frame.add(display, BorderLayout.CENTER);

        ActionListener buttonActionListener = new ButtonListener();
        resetButton = new JButton("Reset Display");
        resetButton.setActionCommand("reset");
        resetButton.addActionListener(buttonActionListener);

        saveButton = new JButton("Save Image");
        saveButton.setActionCommand("save");
        saveButton.addActionListener(buttonActionListener);

        southPanel.add(resetButton);
        southPanel.add(saveButton);
        frame.add(southPanel, BorderLayout.SOUTH);

        frame.pack();
        frame.setVisible(true);
        frame.setResizable(false);

    }

    private void drawFractal(){
        enableUI(false);
        rowsRemaining = size;
        for (int y = 0; y < size; y++){
            FractalWorker fractalWorker = new FractalWorker(y);
            fractalWorker.execute();
        }
    }


    private class ButtonListener implements ActionListener{
        public void actionPerformed(ActionEvent e){
            if (e.getActionCommand().equals("reset")){
                fractal.getInitialRange(range);
                drawFractal();
            }
            if (e.getActionCommand().equals("save")){
                JFileChooser fileChooser = new JFileChooser();
                FileFilter filter = new FileNameExtensionFilter("PNG Images", "png");
                fileChooser.setFileFilter(filter);
                fileChooser.setAcceptAllFileFilterUsed(false);
                int result = fileChooser.showSaveDialog(display);
                if (result == JFileChooser.APPROVE_OPTION){
                    try {
                        ImageIO.write(display.image, "png", fileChooser.getSelectedFile());
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(fileChooser, ex.getMessage(), "Ошибка сохранения", JOptionPane.ERROR_MESSAGE);
                    }
                }

            }
        }
    }

    private class MouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e){
            if (rowsRemaining > 0){
                return;
            }
            int x = e.getX();
            int y = e.getY();

            double xCoord = FractalGenerator.getCoord(range.x, range.x + range.width, size, x);
            double yCoord = FractalGenerator.getCoord(range.y, range.y + range.height, size, y);

            fractal.recenterAndZoomRange(range, xCoord, yCoord, 0.5);
            drawFractal();
        }
    }

    private class FractalWorker extends SwingWorker<Object, Object>{
        private int currentY;
        private ArrayList<Integer> rgbColors;

        FractalWorker(int y){
            this.currentY = y;
        }

        @Override
        protected Object doInBackground() throws Exception {
            rgbColors = new ArrayList<>(size);
            for(int x = 0; x < size; x++){
                double xCoord = FractalGenerator.getCoord(range.x, range.x + range.width, size, x);
                double yCoord = FractalGenerator.getCoord(range.y, range.y + range.height, size, currentY);

                int iterations = fractal.numIterations(xCoord, yCoord);
                int rgbColor;
                if(iterations == -1){
                    rgbColor = 0;
                }
                else {
                    float hue = 0.7f + (float) iterations / 200f;
                    rgbColor = Color.HSBtoRGB(hue, 1f, 1f);

                }
                rgbColors.add(rgbColor);
            }
            return null;
        }

        @Override
        protected void done() {
            for(int x = 0; x < size; x++){
                display.drawPixel(x, currentY, rgbColors.get(x));
            }
            display.repaint(0, 0, currentY, size, 1);
            rowsRemaining--;
            if (rowsRemaining == 0) enableUI(true);
        }
    }

    private void enableUI(boolean val){
        resetButton.setEnabled(val);
        saveButton.setEnabled(val);
        fractalList.setEnabled(val);
    }

    public static void main(String[] args){
        FractalExplorer explorer = new FractalExplorer(800);
        explorer.createAndShowGUI();
        explorer.drawFractal();

    }
}

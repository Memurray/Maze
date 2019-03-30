//Notes

//Window is not resizable

//Each grid element is always square so even though grid block size is dependant on total grid elements, large difference
//in rows vs columns will produce large amount of whitespace

//Stop button really just pauses the animation, becomes a resume button after selecting while running,
// is selectable while nothing is happening but will immediately turn back to "Stop" (likely will never see it say resume here, which is very much intended)

//Solving adheres to left-hand rule

//************Running/Animating******************
//Animation timer basically allows the user to choose slider between 1ms timer tick and 100ms timer tick (inverted so 100 speed is 1ms and 1 speed is 100ms)
//This produces a sufficiently fast running of a 60x60 on a local machine (My average 100 speed animated solve is 7 seconds)
//but!! running on the VM this turns into about a 2 minute run, I dont know if this the due to higher calculation time, or just slower repaint on VM
//As such, I added a button "Finish Animation" which stops the current animation and just finishes it in 1 repaint. It is still the same generation or solve just doesnt wait inbetween steps.
//Honestly added this to solve a last minute issue but it seems like a valuable option even when not needing to wait long times for animation.


import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Maze extends JFrame {

    private Container c;
    private MazeLogic mazePanel;
    private JPanel configPanel, genPanel, solvePanel,speedPanel,sizePanel;
    private JButton generateButton,solveButton, stopButton, finishButton;
    private JLabel speedLabel, rowLabel, colLabel;
    private JSlider speedSlider, rowSlider, colSlider;
    private JCheckBox genBox, solveBox;
    private Timer animationTimer;  //timer for animating generation and solving
    private boolean generating,solving,genClicked,solClicked;


    public Maze() {
        c = getContentPane();
        configPanel = new JPanel();  //overall panel for all config options
        genPanel = new JPanel(); //subpanel of config panel for grouping generating buttons
        solvePanel = new JPanel(); //subpanel of config panel for grouping solving buttons
        speedPanel= new JPanel(); //subpanel of config panel for grouping speed slider and label
        sizePanel = new JPanel();  //subpanel of config panel for grouping size sliders and labels
        mazePanel = new MazeLogic(30,30);  //on load, default Maze will be a 30x30

        animationTimer = new Timer(50, new ActionListener() {  //animation timer by default ticks every 50 ms
            public void actionPerformed(ActionEvent evt) {

                if(genClicked) {  //this is only true if show gen checkbox was selected when gen button was clicked
                    generating = mazePanel.mazeGenStep();  //this function runs 1 step of generation and returns false when done
                    if(!generating){  //if genStep finished
                        animationTimer.stop();  //stop timer
                        genClicked = false;  //turn off clicked flag so it wont keep triggering inner generating logic
                    }
                }
                else if(solClicked) { //this is only true if show solve checkbox was selected when solve button was clicked
                    solving = mazePanel.mazeSolveStep();  //functions runs 1 step of solve and returns false when done
                    if(!solving){  //if solveStep is done
                        animationTimer.stop();  //stop timer
                        solClicked = false;  //disable entry logic
                    }
                }
                else {  //if the timer is somehow ticking but no buttons are considered clicked, turn off timer
                    animationTimer.stop();
                    stopButton.setText("Stop");  //we can get here is start/stop toggle is hit when nothing is happening, so change button text back to stop
                }
            }
        });
        // define generate button
        generateButton = new JButton("Generate");
        generateButton.addActionListener(new ActionListener() { //if genbutton clicked
            public void actionPerformed(ActionEvent e) {
                mazePanel.reDim(colSlider.getValue(),rowSlider.getValue());  //resize view based on new spec from sliders
                solClicked = false;  //automatically kills a solution when new maze is generated
                if(genBox.isSelected()) {  //if show generation is toggled on
                    genClicked = true;  //set gen flag
                    mazePanel.freshStart();  //run combination clear and rebuild function
                    animationTimer.start();  //start animation timer
                }
                else
                    mazePanel.genClick();  //run combination function for instant generating maze
            }
        });
        genBox = new JCheckBox("Show Generation");
        //define solve button
        solveButton = new JButton("Solve");
        solveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(!genClicked) {  //unable to act while generate is still building maze
                    if (solveBox.isSelected()) {  //if show solve is selected
                        solClicked = true;  //set solving flag
                        animationTimer.start();  //start animation timer
                    } else
                        mazePanel.mazeSolve();  //no show means instant solve
                }

            }
        });
        solveBox = new JCheckBox("Show Solver");
        //speed slider logic
        speedLabel = new JLabel("Speed: 50");
        speedSlider = new JSlider(SwingConstants.HORIZONTAL, 1, 100, 50 ); //slider from 1 to 100, starting at 50
        speedSlider.addChangeListener(new ChangeListener(){
                                          public void stateChanged( ChangeEvent e ){  //when slider changed, update tick time
                                              speedLabel.setText("Speed: " + Integer.toString(speedSlider.getValue()));
                                              animationTimer.setDelay(101-speedSlider.getValue());  // the idea here is 100ms tick time at speed 1 and 1ms tick time at speed 100
                                              repaint();
                                          }
                                      }
        );

        finishButton = new JButton("Finish Animation"); //for when you regret selecting the show animation option
        finishButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(genClicked){ //if generating click flag is true
                    genClicked = false;  //stop click flag
                    generating = false; //stop generating flag, unsure if this is needed, but it's 100% safer
                    animationTimer.stop(); //stop timer, based on flag would terminate next cycle anyways, but decided to just stop it
                    mazePanel.mazeGenFinish(); //simple function combined to run standard generate, remove cutting icon and repaint
                }
                if(solClicked){  //if solving click flag is true
                    solClicked = false;  //stop click flag
                    solving = false; //stop solving flag
                    animationTimer.stop(); //stop timer
                    mazePanel.mazeSolveFinish(); //simple combined function to run standard solve and repaint
                }
            }
        });


        //row slider logic
        rowLabel = new JLabel("Rows: 30");
        rowSlider = new JSlider(SwingConstants.HORIZONTAL, 10, 60, 30 );//number of rows ranging from 10 to 60 default to 30
        rowSlider.addChangeListener(new ChangeListener(){
                                        public void stateChanged( ChangeEvent e ){  //when slider changed, update row label
                                            rowLabel.setText("Rows: " + Integer.toString(rowSlider.getValue()));
                                            repaint();
                                        }
                                    }
        );
        //col slider logic
        colLabel = new JLabel("Cols: 30");
        colSlider = new JSlider(SwingConstants.HORIZONTAL, 10, 60, 30 );//number of cols ranging from 10 to 60 default to 30
        colSlider.addChangeListener(new ChangeListener(){
                                        public void stateChanged( ChangeEvent e ){  //when slider changed, update col label
                                            colLabel.setText("Cols: " + Integer.toString(colSlider.getValue()));
                                            repaint();
                                        }
                                    }
        );
        //stop button logic
        stopButton = new JButton("Stop");
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(animationTimer.isRunning()) {  //if running, turn off, change text to inform what clicking will do
                    animationTimer.stop();
                    stopButton.setText("Resume");
                }
                else {  //if stopped, start running, change text to inform what clicking will do
                    animationTimer.start();
                    stopButton.setText("Stop");
                }
            }
        });

        configPanel.setLayout(new GridLayout(5, 1, 2, 50));//layout of config panel

        //add items to genPanel and add genPanel to configPanel
        genPanel.add(generateButton);
        genPanel.add(genBox);
        configPanel.add(genPanel);

        //add items to solvePanel and this to configPanel
        solvePanel.add(solveButton);
        solvePanel.add(solveBox );
        configPanel.add(solvePanel);

        //add items to speedPanel and this to configPanel
        speedPanel.setLayout(new BoxLayout(speedPanel, BoxLayout.PAGE_AXIS));
        speedPanel.add(speedLabel);
        speedPanel.add(speedSlider);
        speedPanel.add(new JLabel(" "));  //blank to add space
        speedPanel.add(finishButton);
        configPanel.add(speedPanel);

        //add items to sizePanel and this to configPanel
        sizePanel.setLayout(new BoxLayout(sizePanel, BoxLayout.PAGE_AXIS));
        sizePanel.add(rowLabel);
        sizePanel.add(rowSlider);
        sizePanel.add(new JLabel(" "));  //blank to add space
        sizePanel.add(colLabel);
        sizePanel.add(colSlider);
        configPanel.add(sizePanel);

        configPanel.add(stopButton);  //stop button freely in configPanel
        c.add(mazePanel.retPanel(), BorderLayout.CENTER); //retrieve jpanel from Mazeview through mazelogic
        c.add(configPanel,BorderLayout.EAST);
        configPanel.setPreferredSize(new Dimension(300,980));
        setSize(1250,980); //designed to accommodate up to 60x60
        setResizable(false); //not resizable
        setVisible(true);
    }

    public static void main(String[] args) {
        Maze mazeQ = new Maze();  //define new Maze object
        mazeQ.addWindowListener(  //watch for window close
                new WindowAdapter(){
                    public void windowClosing(WindowEvent e) {
                        System.exit(0);
                    }
                });
    }
}
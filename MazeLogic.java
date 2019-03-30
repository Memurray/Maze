import java.awt.*;
import java.util.Random;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Stack;

public class MazeLogic {
    Random rand = new Random();
    private Point verts[][];
    private int xDim, yDim;
    private MazeView mw;
    private boolean visited[][];
    private int visitCount[][];
    private int x,y, xSol, ySol, lastDir;
    private Queue<Point> q = new LinkedList<>();
    private Stack<Point> s = new Stack<Point>();

    public MazeLogic(int xSize, int ySize){ //constructor
        xDim  = xSize+1;  //1 more wall segment than number of cells
        yDim = ySize+1;
        verts = new Point[yDim][xDim]; //define new Point matrix
        mw = new MazeView(xSize,ySize); // create new mazeView object
        freshStart(); //run combined function
        repaint(); //combined function called repaint, (repasses relevant variables and triggers mazeView object's repaint)
    }

    public void reDim(int xSize, int ySize){ //instead of building a new object on generate maze, it just changes relevant variables for repaint of old panel
        xDim  = xSize+1;
        yDim = ySize+1;
        verts = new Point[yDim][xDim];
        mw.reDim(xSize,ySize); //needs to trigger equivalent reDim function in mazeView
        freshStart();
        repaint();
    }

    public MazeView retPanel(){return mw.retPanel();}  //returns panel from view to maze


    public void genClick(){//function for instantly generating maze
        freshStart();  //delete old
        mazeGen(); //run generation function
        repaint();
    }

    public void breakWall(int x, int y, int select){//given current cell's (x,y) and which wall you want to break (North=0,East=1,South=2,West=3)
        x--; //adjust index  (want to refer to first block as (1,1) but arrays start at 0)
        y--;
        if (select == 0 && y>0) //if north and y within render space
            verts[y][x].x = 0;  //no horizontal line to right neighbor vertex
        else if (select == 1 && x< xDim-2)  //if east and x within render space
            verts[y][x+1].y = 0;  //right neighbor vertex has no vertical line connect south
        else if (select == 2 && y< yDim-2) //if south and y within render space
            verts[y+1][x].x = 0; //south neighbor has no horizontal line connecting to right
        else if (select == 3 && x>0) // if west and within render space
            verts[y][x].y = 0;  //no vertical line connecting to south vertex
        //essentially each point only has a top and left wall, but the equivalent right wall is right neighbors left wall.
    }

    public void setupVisted() { //create core 2-d arrays for modeling spatially encoded decision flags
        visited = new boolean[yDim + 1][xDim + 1];
        visitCount = new int[yDim + 1][xDim + 1];

        for (int i = 0; i <= yDim; i++) {
            for (int j = 0; j <= xDim; j++) {
                if (i == 0 || j == 0 || i == yDim || j == xDim) {  //border ring around render space, dont actually physically exist
                    visited[i][j] = true; //considered as visited so pathing cant violate outer wall
                    visitCount[i][j] = 3; //considered as 3 so, cant be visited for solving and wont render fill
                } else {
                    visited[i][j] = false; //not visited
                    visitCount[i][j] = 0; //been visited 0 times
                }
            }
        }
        visited[1][1] = true; //entry spot starts as visited
    }

    public void mazeGen(){ //rules for automatic full maze gen
        while(true) {
            q.add(new Point(x, y)); //add current spot to queue
            int openNeighbors = validNeighbors(x, y);  //calculate number of valid neighbors
            if (openNeighbors < 1) {  //if no valid neighbors
                while (q.peek() != null && openNeighbors < 1) { //while q isnt empty and current inspected node has no openNeighbors
                    Point newXY = q.remove();  //grab top of queue
                    x = newXY.x;  //set this new point as new x and y
                    y = newXY.y;
                    openNeighbors = validNeighbors(x, y);  //calculate number of neighbors of this point
                }
                if (q.peek() == null && openNeighbors < 1) {  //if q empty and no valid neighbors, we're done
                    break;
                }
            } else {//if there are open neighbors
                int direction = pickNeighbor(x, y, openNeighbors);  //randomly pick one of the valid neighbors
                breakWall(x, y, direction);  //break that wall
                if (direction == 0)  //move current inspection location based on which direction was chosen
                    y--;
                else if (direction == 1)
                    x++;
                else if (direction == 2)
                    y++;
                else
                    x--;
                visited[y][x] = true;  //mark new node as visited
            }
        }
    }

    public boolean mazeGenStep(){  //identical formulation as mazeGen() but removed the loop and added bool return to allow calling timer to know when done
        q.add(new Point(x, y));
        int openNeighbors = validNeighbors(x, y);
        if (openNeighbors < 1) {
            while (q.peek() != null && openNeighbors < 1) {
                Point newXY = q.remove();
                x = newXY.x;
                y = newXY.y;
                openNeighbors = validNeighbors(x, y);
            }
            if (q.peek() == null && openNeighbors < 1) {
                mw.setSinglePaint(new Point(-1, -1));
                repaint();  //even on fail we need to render last point
                return false;  //generating flag in Maze.java will be made false
            }
        } else {
            mw.setSinglePaint(new Point(x-1, y-1));  //send current point off to view render so that we can more easily visualize where path cutting is at
            int direction = pickNeighbor(x, y, openNeighbors);
            breakWall(x, y, direction);
            if (direction == 0)
                y--;
            else if (direction == 1)
                x++;
            else if (direction == 2)
                y++;
            else
                x--;
            visited[y][x] = true;
        }
        repaint(); //repaints each step
        return true;
    }

    public void mazeGenFinish(){ //used to finish partially animated maze generation
        mazeGen();
        mw.setSinglePaint(new Point(-1, -1));
        repaint();
    }

    public void mazeSolve(){ //logic for solving the maze
        while(true) {
            if(ySol == yDim-1 && xSol == xDim-1){  // if at end of the maze
                visitCount[ySol][xSol] = 1;  //paint last tile as green path
                mw.setVisitCount(visitCount);  //update render control variable
                repaint();
                break;  //done
            }
            if (otherPath()) {  //if at least one neighbor that is accessible has not been visted then
                s.add(new Point(xSol, ySol));  //add point to stack
                visitCount[ySol][xSol] = 1;  //mark as green path


                if (pathOpen((lastDir + 3) % 4)) { //check if we can turn left first (this is to enforce left hand rule)
                    moveSol((lastDir + 3) % 4);  //move into that node
                    lastDir = (lastDir + 3) % 4;  //update the last direction variable with left turn relative to previous facing
                    // (direction +1 is equal to turning right based on my directional encoding, to avoid negative numbers, turning left is equal to turning right 3 times)
                } else if (pathOpen(lastDir)) //if left not open but straight is
                    moveSol(lastDir);
                else if (pathOpen((lastDir + 1) % 4)) {  //if left and straight not open but right is
                    moveSol((lastDir + 1) % 4);
                    lastDir = (lastDir + 1) % 4; //update direction to direction + 1, modulo to allow direction 3 to become direction 0
                }
            } else { //if hit dead end
                visitCount[ySol][xSol] = 2;  //mark current spot as red path
                Point newXY = s.pop();  //pop stack and capture last point on path
                //consider the direction that is being taken based on where current to next node moves
                if (newXY.y - ySol == -1) //if newY is 1 less than old, went north (0)
                    lastDir = 0;
                else if (newXY.x - xSol == 1)
                    lastDir = 1;
                else if (newXY.y - ySol == 1)
                    lastDir = 2;
                else
                    lastDir = 3;
                //maintaining this new direction means that we are essentially still able to abide by left hand rule the moment a popped node has valid path

                xSol = newXY.x; //update solution x,y to new values
                ySol = newXY.y;
            }
            mw.setVisitCount(visitCount); //update render control structure in view
            repaint(); //repaint
        }
    }

    public boolean mazeSolveStep(){ //basically identical to mazeSolve() function without the loop, returns t/f based on if still running and repaints every cycle
        if(ySol == yDim-1 && xSol == xDim-1){
            visitCount[ySol][xSol] = 1;
            mw.setVisitCount(visitCount);
            repaint();
            return false; //if reached the end of the maze, let calling function know to stop
        }
        if(otherPath()){ //if there is a valid next square
            s.add(new Point(xSol,ySol));
            visitCount[ySol][xSol] = 1;

            if(pathOpen((lastDir+3)%4)) { //check left first
                moveSol((lastDir + 3) % 4);
                lastDir = (lastDir + 3) % 4;
            }
            else if(pathOpen(lastDir))  //if not left then straight
                moveSol(lastDir);
            else if (pathOpen((lastDir+1)%4)) { // if not straight then right
                moveSol((lastDir + 1) % 4);
                lastDir = (lastDir + 1) % 4;
            }
        }
        else{ //if need to backtrack
            visitCount[ySol][xSol] = 2;
            Point newXY = s.pop();
            if(newXY.y - ySol == -1) //track direction backtrack takes us
                lastDir = 0;
            else if(newXY.x - xSol == 1)
                lastDir = 1;
            else if(newXY.y - ySol == 1)
                lastDir = 2;
            else
                lastDir = 3;

            xSol = newXY.x;
            ySol = newXY.y;
        }
        mw.setVisitCount(visitCount);
        repaint();
        return true;
    }

    public void mazeSolveFinish(){ //used to finish partially animated solve
        mazeSolve();
        repaint();
    }

    public boolean otherPath(){  //returns true if any of the neighbors have an open path and have not been visited (for solving)
        if(visitCount[ySol+1][xSol] == 0 && verts[ySol][xSol-1].x == 0)
            return true;
        if(visitCount[ySol-1][xSol] == 0 && verts[ySol-1][xSol-1].x == 0)
            return true;
        if(visitCount[ySol][xSol+1] == 0 && verts[ySol-1][xSol].y == 0)
            return true;
        if(visitCount[ySol][xSol-1] == 0 && verts[ySol-1][xSol-1].y == 0)
            return true;
        return false;
    }

    public boolean pathOpen(int select){ //extra step over otherPath() where validation requires the proposed directional choice to match with what is open
        if (visitCount[ySol-1][xSol] == 0 && select == 0 && verts[ySol-1][xSol-1].x == 0)
            return true;
        else if (visitCount[ySol][xSol+1] == 0 && select == 1 && verts[ySol-1][xSol].y == 0)
            return true;
        else if (visitCount[ySol+1][xSol] == 0 && select == 2 && verts[ySol][xSol-1].x == 0)
            return true;
        else if (visitCount[ySol][xSol-1] == 0  && select == 3 && verts[ySol-1][xSol-1].y == 0)
            return true;
        else
            return false;
    }

    public void moveSol (int select){ //updates xSol or ySol based on what direction is passed into the function
        if(select==0)        //north
            ySol--;
        else if(select == 1) //east
            xSol++;
        else if(select == 2) //south
            ySol++;
        else                 //west
            xSol--;
    }

    public int validNeighbors(int x, int y){ //For generation, determine the number of neighbors have yet to be visited
        int count = 0;
        if(!visited[y+1][x])
            count++;
        if(!visited[y-1][x])
            count++;
        if(!visited[y][x+1])
            count++;
        if(!visited[y][x-1])
            count++;
        return count;
    }

    public int pickNeighbor(int x, int y, int totalN){ //given x,y coord and the number of valid neighbors, generates a value
        // between 0 and (valid neighbors -1) returning the direction relating to which direction matches the nth valid direction
        int  randPick = rand.nextInt(totalN);

        if(!visited[y-1][x]){
            if (randPick == 0) //if we picked 0 and this neighbor is valid, go north
                return 0;
            randPick = randPick -1; //if valid neighbor but random number wasnt 0, decrement random number
        }

        if(!visited[y][x+1]){ //if valid neighbor
            if (randPick == 0)  // if rand value is now 0
                return 1; //go east
            randPick = randPick -1; //otherwise decrement random number
        }

        if(!visited[y+1][x]){  //if valid neighbor
            if (randPick == 0) //if rand is now 0
                return 2;  //go south
            randPick = randPick -1;  //if not, decrement random number
        }

        if(!visited[y][x-1]){  // honestly could just be an else
            if (randPick == 0)  //if rand is now 0
                return 3; // go west
            randPick = randPick -1;
        }
        return -1; //this return should never occur, but if it does, this is an easy check for debugging
    }

    public void freshStart(){  //combined function to make reseting everything easier
        x=1;  //reset generating x,y
        y=1;
        xSol=1; // reset solution x,y
        ySol=1;
        lastDir = 1;  //assuming first entry is from left of cell (1,1) therefore moving east
        q = new LinkedList<>(); //clear queue
        s = new Stack<Point>(); //clear stack
        blankGrid();  //clears vert (vertex rending structure)
        setupVisted(); //empties out main grid area of visited and defines invisible outer bounds
    }

    public void blankGrid(){
        for(int i =0; i < yDim; i++){ //for all point in 2-d array
            for(int j =0; j < xDim; j++){
                verts[i][j]= new Point(1,1);  //define point as having both a horizontal and vertical line
            }
        }
    }

    public void repaint(){
        mw.setVisitCount(visitCount);  //update visitCount structure in view
        mw.setVerts(verts); //update vert structure in view
        mw.repaint(); //force a panel repaint
    }}
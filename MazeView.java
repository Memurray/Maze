import java.awt.*;
import javax.swing.*;
import java.awt.Point;

public class MazeView extends JPanel{
    private int GRID_OFFSET;  //core element render size, each block will be 15 pixels by 15 pixels
    private int xDim, yDim;
    private Point verts[][]; //holds vertices to be rendered
    private Point singlePaint;  //holds x,y of single point to render for showing generation path cutting
    private int visitCount[][]; //spatially encoded visitCount[y][x] for coordinate(x,y) holding number of times been visited,
    // will render that coord as green if visited once and red if visited twice, other values are ignored
    private JLabel visited;  //visited percent display is a part of this frame


    public MazeView(int xSize,int ySize){ //constructor
        xDim  = xSize+1;  //rendering structure uses 1 more vertex in each x and y direction
        yDim = ySize+1;
        GRID_OFFSET = (int)Math.floor(900 / max(xSize,ySize)); //generally fill render area with maze, element always square and always integer size
        singlePaint = new Point(-1,-1);  //single paint defaults to outside render space
        setBackground (Color.WHITE);
        visited = new JLabel("Visited: 0%", JLabel.CENTER); //visted label in center (left to right)
        visited.setFont(new Font("Serif", Font.PLAIN, 24));  //larger font
        setLayout(new BorderLayout());
        add(visited, BorderLayout.SOUTH); //visited label in south (up to down)
    }

    public void reDim(int xSize,int ySize){//When render view size specifications change we need to reinitialized the objects core parameters
        xDim  = xSize+1;
        yDim = ySize+1;
        GRID_OFFSET = (int)Math.floor(900 / max(xSize,ySize)); //generally fill render area with maze, element always square and always integer size
        singlePaint = new Point(-1,-1);
        setBackground (Color.WHITE);
    }

    public double max(int col, int row){ //return whichever size is larger, used to determine best larger square element size allowed
        if (row>col)
            return row;
        else
            return col;
    }

    public void setVerts(Point inVerts[][]){verts = inVerts; } //set verts so that logic can update this value used for rendering

    public void setVisitCount(int vC[][]){visitCount= vC;}  //set visitCount so that logic can update this value used for rendering

    public int computeVisitedPercent(){//easier to do here
        double sum=0;
        for(int i =1; i< yDim; i++){  //visitedCount uses a dummy border so we want to start at 1 instead of 0
            for(int j =1; j< xDim; j++) {
                if(visitCount[i][j]>0) //
                    sum ++; //count cells that have been visited
            }
        }
        return (int)(sum/(xDim-1)/(yDim-1)*100);  //number of cells visited / total cells * 100 = visited percent
    }

    public void setSinglePaint(Point single){singlePaint = single;}  //set function for allowing logic to update this value

    public MazeView retPanel(){
        return MazeView.this;
    }  //send back this panel so it can be displayed in core frame

    public void paintComponent(Graphics g){  //each paint
        super.paintComponent(g);
        visited.setText("Visited: " + computeVisitedPercent() + "%");  //write new visited
        Graphics2D g2 = (Graphics2D) g;
        for(int i =0; i< yDim; i++){ //for each render block
            for(int j =0; j< xDim; j++) {
                if (singlePaint.x == j && singlePaint.y == i) { //if single point is a match to current render block, paint blue filled square
                    g.setColor(Color.BLUE);
                    g.fillRect( j * GRID_OFFSET,  i * GRID_OFFSET, GRID_OFFSET , GRID_OFFSET );
                }
                if (visitCount[i+1][j+1] == 1) {  //if visit count of matched render block is 1, paint green filled square
                    g.setColor(Color.GREEN);
                    g.fillRect( j * GRID_OFFSET,  i * GRID_OFFSET, GRID_OFFSET , GRID_OFFSET );
                }
                else if (visitCount[i+1][j+1] == 2) {  //if visit count of matched render block is 2, paint red filled square
                    g.setColor(Color.RED);
                    g.fillRect( j * GRID_OFFSET,  i * GRID_OFFSET, GRID_OFFSET , GRID_OFFSET );
                }
                g.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(2));
                // vert.x = 1 means that it has a horizontal line drawn to it's right neighbor, 0 means no line
                // vert.y = 1 means that it has a vertical line drawn to it's south neighbor
                if (verts[i][j].y == 1 && i < yDim - 1)  //vertical border lines (walls)
                    g2.drawLine(j * GRID_OFFSET, i * GRID_OFFSET, j * GRID_OFFSET, (i + 1) * GRID_OFFSET);
                if (verts[i][j].x == 1 && j < xDim - 1) //horizontal border lines (walls)
                    g2.drawLine(j * GRID_OFFSET, i * GRID_OFFSET, (j+1) * GRID_OFFSET, i * GRID_OFFSET);
            }
        }
    }
}
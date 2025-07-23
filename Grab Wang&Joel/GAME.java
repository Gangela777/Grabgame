/*
No.7 Grab
Group members:
    WANG YIFAN           U2420354J email:wang2124
    JOEL RAFAEL SUTANTO  U2422374L email:jsutanto002
PS:Prof we're sorry that there are still problems with the undo/redo part but we tried our best and didn't have more time for it. Please help us and fix the bug.
*/

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.util.*;

public class GAME 
{
	//for text and display//
	private static void print(JTextPane textpane,String html)
	{
		textpane.setContentType("text/html");
		textpane.setText(html);
	}
	
	private static JTextPane newtextdisplay()
	{
		JTextPane disp=new JTextPane();
		disp.setFocusable(false);//make not focusable (and not editable)
		disp.setBackground(null);//make transparent
		DefaultCaret caret=new DefaultCaret();
		caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
		disp.setCaret(caret);//stop scrollpane from tracking the caret in (disp)
		return disp;
	}
	
	//for overall UI//
	private final Dimension winsize=new Dimension(840, 770);
    private final JFrame win=new JFrame("GAME");
    private final JPanel game=new JPanel();
    private final JPanel board=new JPanel();
    private final JPanel control=new JPanel();
    private final JTextPane status;
    private final int statusheight=70;
    private final JTextField inputSize=new JTextField(5);
	private final JButton go=new JButton("Go");
	private String player;
	private int moves;
	private final JButton undoBtn=new JButton("Undo");
	private final JButton redoBtn=new JButton("Redo");
    
    //for meeples//
    private final JPanel Xmeeple=new JPanel();
    private final JPanel Omeeple=new JPanel();
    private int xX,yX;
    private int xO,yO;
    
    //for gaming initializing//
    private int n=6;//default size
    private JPanel[][] cells;
    private final int cellwidth=49;
    private final int cellspacing=4;
    private final int boxwidth=cellwidth+cellspacing;
    private final Dimension boxsize=new Dimension(boxwidth,boxwidth);
    private final Dimension cellsize=new Dimension(cellwidth,cellwidth);
    private int[][] numbers=new int[n][n];
    
    //for counting valid clicks//
    private int count;
    
    //for calculating scores//
    private int SX,SO;
    
    //for checking if game ends//
    private boolean gameEnd=false;
    
    //for undo and redo//
    private final ArrayList<GameState> history=new ArrayList<>();
    private final ArrayList<GameState> future=new ArrayList<>();
    
    public GAME()
    {
    	//set up the window//
        win.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        win.setPreferredSize(winsize);
        
        //set up bars and panels//
        win.add(new JScrollPane(game));
        game.setFocusable(true);
        game.setLayout(new GridBagLayout());
        GridBagConstraints gbc=new GridBagConstraints();
        gbc.ipadx=7; 
        gbc.ipady=7; 
        gbc.gridx=1; 
        gbc.gridy=1;
        game.add(board,gbc);
        gbc.gridy++;
        control.setLayout(new GridBagLayout());
        game.add(control,gbc);
        gbc.gridy++;
        status=new JTextPane();
        game.add(status,gbc);
        gbc.gridy++;
        
        //display the window//
        win.pack(); // packs the contents of (win)
        win.setVisible(true);
        
        //make meeples//
        Xmeeple.setBackground(null);
        Omeeple.setBackground(null);
        JLabel a=new JLabel("X");
        a.setFont(new Font("Arial",Font.BOLD,32));//from Google since I want the meeples to be bigger
        Xmeeple.add(a);
        JLabel b=new JLabel("O");
        b.setFont(new Font("Arial",Font.BOLD,32));
        Omeeple.add(b);
		
		initialize();
		board.revalidate();
        board.repaint();
		
		//set up control panel//
    	control.setLayout(new GridBagLayout());
    	{
    	    DefaultListener setSize=new DefaultListener() 
    	    {
    	        public void actionPerformed(ActionEvent e) 
    	        {
    	            int newSize=Integer.parseInt(inputSize.getText());
    	            if (newSize>=4) 
    	            {
    	            	if(newSize%2==0)
    	            	{
    	            		n=newSize;
    	                    numbers=new int[n][n];
    	                    initialize();
    	                    board.revalidate();
    	                    board.repaint();
    	            	}
    	            	else JOptionPane.showMessageDialog(win,"Please input even number >=4!");
    	            } 
    	            else JOptionPane.showMessageDialog(win,"Please input even number >=4!");
    	         } 
    	    };
    	    inputSize.addActionListener(setSize);
    	    go.addActionListener(setSize);
    	    undoBtn.addActionListener(e -> undo());
    	    redoBtn.addActionListener(e -> redo());
    	    GridBagConstraints ctrl=new GridBagConstraints();  
    	    ctrl.gridx=1;
    	    ctrl.gridy=1;
    	    control.add(new JLabel("Board Size:"),ctrl);
    	    ctrl.gridx++;
    	    control.add(inputSize, ctrl);
    	    ctrl.gridx++;
    	    control.add(go, ctrl);
    	    ctrl.gridx++;
    	    control.add(undoBtn, ctrl);
    	    ctrl.gridx++;
    	    control.add(redoBtn, ctrl);
    	}
    	game.add(control,gbc);
		
		//remove focus on click outside the board and control//
		Toolkit.getDefaultToolkit().addAWTEventListener
		(
			new AWTEventListener()
			{
				public void eventDispatched(AWTEvent e)
				{
					if(e instanceof MouseEvent && e.getID()==MouseEvent.MOUSE_PRESSED)
					{
						Object src=e.getSource();
						if(src instanceof Component)
						{
							Component component=(Component)src;
							boolean clickboard=board.isAncestorOf(component);
							boolean clickcontrol=control.isAncestorOf(component);
							if(clickboard||control.isAncestorOf(component)) return;
							game.requestFocus();
						}
					}
				}
			}
		    ,AWTEvent.MOUSE_EVENT_MASK
		);
		
		//make key listener//
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher
		(
			
		    new KeyEventDispatcher()
		    {
			    public boolean dispatchKeyEvent(KeyEvent e)	
			    {
				    Component component=e.getComponent();	// usually the currently focused component
				    int code=e.getKeyCode();
				    int mod=e.getModifiersEx();
				    boolean ctrl=0<(mod&KeyEvent.CTRL_DOWN_MASK);
				    boolean shift=0<(mod&KeyEvent.SHIFT_DOWN_MASK);
				    boolean alt=0<(mod&KeyEvent.ALT_DOWN_MASK);
				    String key=(ctrl?"ctrl ":"")+(shift?"shift ":"")+(alt?"alt ":"")+code;
				    if( e.getID()==KeyEvent.KEY_PRESSED )
				    {
					    if(key.equals("ctrl 78"))//initializes on Ctrl+N
					    {
					    	initialize();
					    	board.revalidate();
					    	board.repaint();
					    }	
					    if (key.equals("ctrl 90"))//Ctrl+Z
					    { 
					    	undo();
					    	return true;
					    }
					    if (key.equals("ctrl 89"))//Ctrl+Y
					    { 
					    	redo();
					    	return true;
					    }
					    if(control.isAncestorOf(component))
					    {
						    if(key.equals("27")) game.requestFocus();// to remove focus from form elements
						    if(key.equals("10")) initialize();// initalizes on Enter within (control)
						    if((code==38||code==40)&&component instanceof JTextField) return true;	//gobbles up/down keys in JTextField
						    return false;//do not process key
					    }
				    }
					return false;// do not process key
			    }
		    }
	    );
	}
    
    private void initialize()
    {
    	//set up basic tools//
    	gameEnd=false;
    	count=0;
    	SX=0;
    	SO=0;
    	
    	//make new board//
    	cells=new JPanel[n][n];
    	board.removeAll();//remove old one to make a new one
        board.setLayout(new GridBagLayout());
    	board.setBackground(Color.black);
    	board.setBorder(BorderFactory.createLineBorder(Color.darkGray,2));//from Google since I wanna make it cool
    	GridBagConstraints gbc=new GridBagConstraints();
    	gbc.ipadx=2;
    	gbc.ipady=2;
    	for(int a=0;a<n;a++) for(int s=0;s<n;s++)
    	{
    	    //make board cells//
    	    JPanel cell=new JPanel();
    	    cells[a][s]=cell;
    	    cell.setMinimumSize(cellsize);
    	    cell.setPreferredSize(cellsize);
    	    cell.setBackground(null);
    	    cell.setLayout(new GridBagLayout());
    	    
    	    //add random numbers to the cells//
    	    if ((a==0&&s==0)||(a==n-1&&s==n-1)) numbers[a][s]=0;
    	    else
    	    {
    	    	int rand=new Random().nextInt(5)+1;
    	    	numbers[a][s]=rand;
        	    JLabel randNum=new JLabel(String.valueOf(rand));
        	    randNum.setFont(new Font("Arial",Font.BOLD,32));
        	    cells[a][s].add(randNum);
    	    }
    	    
    	    //put the (cell) into a bigger (box) to make cell spacing and borders//
    	    JPanel box=new JPanel();
    	    box.setPreferredSize(boxsize);
    	    box.setLayout(new GridBagLayout());
    	    box.setBorder(new BevelBorder(BevelBorder.RAISED));
    	    box.add(cell);
    	    gbc.gridx=s;
    	    gbc.gridy=a;
    	    board.add(box,gbc);
    	    box.setBackground(a%2==s%2?Color.yellow:Color.blue);
    	    
    	    //add mouse listener//
    	    final int x=a,y=s;
			box.addMouseListener
			(
				new DefaultListener()
				{
					public void mousePressed(MouseEvent e)
					{
						if(e.getButton()==1)
						{	
							if(validClick(x,y)) 
							{
								jump(x,y);
								count++;
								if(count>=5) count=1;//reset so it's easier to do coding
							}
							game.requestFocus();//remove focus from form elements
						}
					}
				}
			);
        }
    	
    	//set default positions of meeples//
    	xX=0;
    	yX=0;
    	xO=n-1;
    	yO=n-1;
    	cells[0][0].add(Xmeeple);
    	cells[n-1][n-1].add(Omeeple);
    	
    	int boardwidth=board.getPreferredSize().width;
    	//resize status//
    	status.setPreferredSize(new Dimension(boardwidth,statusheight));
    	
    	//make sure that can undo first step
    	saveState();
    }
    
    private boolean validClick(int x,int y) 
    {
    	if(gameEnd) return false;	

    	//check whether there is already a meeple in the cell//
    	if(cells[x][y].isAncestorOf(Xmeeple)||cells[x][y].isAncestorOf(Omeeple)) return false; 

    	//check if the click is within the area//
        if(count==0||count==3||count==4)//due to the order of the code, count here should be different
        {
        	if(Math.abs(xX-x)>0&&Math.abs(yX-y)>0) return false;
        }
        if(count==1||count==2)
        {
        	if(Math.abs(xO-x)>0&&Math.abs(yO-y)>0) return false;
        }
        if(count==5) return false;//in case if sth goes wrong, though this case won't happen
        
        //check whether there is a number in the cell//
        if(numbers[x][y]==0) return false;
        
        return true;
    }
    
    private void jump(int x,int y) 
    {
        saveState();
    	if(gameEnd) return;
        if(count==1||count==2)
        {
            xO=x;//update the current meeple's position
            yO=y;
            cancelNum(xO,yO);
            cells[xO][yO].add(Omeeple);
            board.revalidate();
            board.repaint();
            SO+=numbers[xO][yO];
            refreshStatus();
            numbers[xO][yO]=0;
        }
        else if(count==0||count==3||count==4)
        {
            xX=x;
            yX=y;
            cancelNum(xX,yX);
            cells[xX][yX].add(Xmeeple);
            board.revalidate();
            board.repaint();
            SX+=numbers[xX][yX];
            refreshStatus();
            numbers[xX][yX]=0;
        }
        checkEnd();
    }
    
    //sometimes when there are still numbers left but players can't move, in such case need to check//
    private boolean isPlayerX()//check who the current player is 
    {
        if(count==1||count==2) return false;
        return true;
    }
    
    private void checkEnd()
    {
        boolean xMove=canMove(true);
        boolean oMove=canMove(false);
        boolean currentIsX=isPlayerX();
        
        //check if there is any number left//
        boolean noNumbers=true;
        for (int[] row:numbers) 
        {
            for (int value:row) 
            {
                if (value!=0) 
                {
                    noNumbers=false;
                    break;
                }
            }
            if (!noNumbers) break;
        }
        
        //check whether can move//
        if (currentIsX&&!xMove)//check current meeple
        {
            end();
            return;
        }
        if (!currentIsX&&!oMove) 
        {
            end();
            return;
        }
        if ((!xMove&&!oMove)||noNumbers) end();//check both  
    }
    
    private boolean canMove(boolean isX)
    {
        int row=isX?xX:xO;
        int col=isX?yX:yO;

        //check if there are numbers available
        for (int j=0;j<n;j++) 
        {
            if(j==col) continue;
            if(numbers[row][j]!=0&&!cells[row][j].isAncestorOf(Xmeeple)&&!cells[row][j].isAncestorOf(Omeeple)) return true;
        }
        for (int i=0;i<n;i++) 
        {
            if(i==row) continue;
            if(numbers[i][col]!=0&&!cells[i][col].isAncestorOf(Xmeeple)&&!cells[i][col].isAncestorOf(Omeeple)) return true;
        }
        
        //if nothing available then no more steps
        return false;
    }
    
    private void cancelNum(int x,int y)//I could't use remove at first so I googled and knew the functions of JPanel
    {
    	Component[] num=cells[x][y].getComponents();
        for (Component c:num)
        {
            if (c instanceof JLabel) cells[x][y].remove(c);
        }
    }
    
    private void refreshStatus() 
    {
    	 if(count==0||count==3||count==4) player="X's turn";
    	 if(count==1||count==2) player="O's turn";
    	 if(count==3||count==1||count==5) moves=2;
    	 if(count==4||count==2) moves=1;
    	 String text="<html><div style='text-align:center;'>"+"<div>"+SX+" : " +SO+"</div>"+"<div>"+player+"</div>"+"<div>Moves remaining: "+moves+"</div>"+"<div>count:"+count+"</div>"+"</div></html>";
    	 print(status, text);
    }
    
    private int checkWin()
    {
    	if(SX>SO) return 1;
    	if(SX<SO) return -1;
    	return 0;
    }
    
    private void end()
    {
        gameEnd=true;
        int result=checkWin();
        String winner;
        if (result==1)winner="<span style='color:blue;'>X wins!</span>";
        else if(result==-1)winner="<span style='color:red;'>O wins!</span>";
        else winner="<span style='color:gray;'>It's a draw!</span>";
        String finalText="<html><div style='text-align:center;'>"+"<div>"+SX+" : "+SO+"</div>"+"<div>"+winner+"</div>"+"<div>Game Over</div>"+"</div></html>";
        print(status, finalText);
    }
    
    private void saveState() 
    {
    	history.add(new GameState(xX,yX,xO,yO,numbers,SX,SO,count));
    	future.clear();//once take steps, just clear redo
    }
    
    private void undo() 
    {
    	if(history.size()<=1) return;
    	GameState last=history.remove(history.size()-1);
    	future.add(last);
    	GameState current=history.get(history.size()-1);
    	restoreState(current);
    }
    
    private void redo() 
    {
    	if(future.isEmpty()) return;
    	GameState next=future.remove(future.size()-1);
    	history.add(next);
    	restoreState(next);
    }
    
    private void restoreState(GameState state) 
    {
    	xX=state.xX;
    	yX=state.yX;
    	xO=state.xO;
    	yO=state.yO;
    	SX=state.SX;
    	SO=state.SO;
    	count=state.count;
    	numbers=new int[n][n];
    	for(int i=0;i<n;i++) System.arraycopy(state.numbers[i],0,numbers[i],0,n);
    	
    	//rebuild the whole board//
    	for(int i=0;i<n;i++) for(int j=0;j<n;j++) 
    	{
    		cells[i][j].removeAll();
    		if (numbers[i][j]>0) 
    		{
    			JLabel figure = new JLabel(String.valueOf(numbers[i][j]));
    			figure.setFont(new Font("Arial",Font.BOLD,32));
    			cells[i][j].add(figure);
    		}
    	}
    	cells[xX][yX].add(Xmeeple);
    	cells[xO][yO].add(Omeeple);
    	board.revalidate();
    	board.repaint();
    	refreshStatus();
    	gameEnd=false;
    }


    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(new Runnable() { public void run() { new GAME(); } });
    }
}

interface DefaultListener extends MouseListener,MouseWheelListener,MouseMotionListener,KeyListener,ActionListener 
{
    default public void mouseClicked(MouseEvent e) {}
    default public void mousePressed(MouseEvent e) {}
    default public void mouseReleased(MouseEvent e) {}
    default public void mouseEntered(MouseEvent e) {}
    default public void mouseExited(MouseEvent e) {}
    default public void mouseMoved(MouseEvent e) {}
    default public void mouseDragged(MouseEvent e) {}
    default public void mouseWheelMoved(MouseWheelEvent e) {}
    default public void keyTyped(KeyEvent e) {}
    default public void keyPressed(KeyEvent e) {}
    default public void keyReleased(KeyEvent e) {}
    default public void actionPerformed(ActionEvent e) {}
}
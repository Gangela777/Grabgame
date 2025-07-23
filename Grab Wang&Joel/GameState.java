/*
No.7 Grab
Group members:
    WANG YIFAN           U2420354J email:wang2124
    JOEL RAFAEL SUTANTO  U2422374L email:jsutanto002
*/

public class GameState 
{
	int xX,yX,xO,yO;
	int[][] numbers;
	int SX,SO,count;

	GameState(int xX,int yX,int xO,int yO,int[][] numbers,int SX,int SO,int count) 
	{
		int n=numbers.length;
		this.xX=xX;
		this.yX=yX;
		this.xO=xO;
		this.yO=yO;
		this.numbers=new int[n][n];
		for (int i=0;i<n;i++) System.arraycopy(numbers[i],0,this.numbers[i],0,n);
		this.SX=SX;
		this.SO=SO;
		this.count=count;
	}
}
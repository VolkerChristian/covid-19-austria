import com.wolfram.jlink.*;

public class Mathematica extends MathJFrame {
	
	public Mathematica(String[] args) {
		KernelLink ml = null;
		MathGraphicsJPanel graphics = new MathGraphicsJPanel();
		
		try {
			ml = MathLinkFactory.createKernelLink(args);
		} catch (MathLinkException e) {
			System.out.println("Fatal error opening link: " + e.getMessage());
		}
		
		try {
			ml.discardAnswer();
			
			ml.evaluate("2+2");
			
			ml.waitForAnswer();
			
			int result = ml.getInteger();
	        System.out.println("2 + 2 = " + result);
			
			graphics.setLink(ml);
			graphics.setMathCommand("Plot[x, {x, 0, 1}]");
			graphics.repaintNow();
			System.out.println(graphics.getMathCommand());
		} catch (MathLinkException e) {
			System.out.println("Fatal error opening link: " + e.getMessage());
		} finally {
			ml.close();
		}
	}
}

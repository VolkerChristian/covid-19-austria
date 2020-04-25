import com.wolfram.jlink.*;

public class SampleProgram {

    public static void main(String[] argv) throws ExprFormatException {
        KernelLink ml = null;

        try {
            ml = MathLinkFactory.createKernelLink(argv);
        } catch (MathLinkException e) {
            System.out.println("Fatal error opening link: " + e.getMessage());
            return;
        }

        try {
            // Get rid of the initial InputNamePacket the kernel will send
            // when it is launched.
            ml.discardAnswer();

            ml.evaluate("<<MyPackage.m");
            ml.discardAnswer();

            ml.evaluate("EmpiricData = {{0,2}, {1,2}, {2,3}, {3,3}, {4,9}, {5,14}, {6,18}, {7,21}}");
            ml.discardAnswer();
            
            ml.evaluate("LogisticModel = a / (1 + b * Exp[-c (x - d)])");
            ml.discardAnswer();
            
            String out = ml.evaluateToOutputForm(
            		"LogisticModelFitParameter = FindFit[EmpiricData, LogisticModel, {a, b, c, d}, x]", 0);
            System.out.println("Res: " + out);
            
            ml.evaluate("LogisticModelFitParameter = FindFit[EmpiricData, LogisticModel, {a, b, c, d}, x]");
            ml.waitForAnswer();
            Expr exp = ml.getExpr();
            System.out.println("Exp a: " + exp.part(1));
            
            ml.evaluate("a /. " + exp.part(1));
            ml.waitForAnswer();
            double a = ml.getDouble();
            
            ml.evaluate("b /. " + exp.part(2));
            ml.waitForAnswer();
            double b = ml.getDouble();
            
            ml.evaluate("c /. " + exp.part(3));
            ml.waitForAnswer();
            double c = ml.getDouble();

            ml.evaluate("d /. " + exp.part(4));
            ml.waitForAnswer();
            double d = ml.getDouble();
            
            System.out.println("a = " + a + ", b = " + b + ", c = " + c + ", d = " + d);
            
            /*
            System.out.println("Exp b: " + exp.part(2));
            System.out.println("Exp c: " + exp.part(3));
            System.out.println("Exp d: " + exp.part(4));
            */
//            double[][] a = ml.getDoubleArray2();
            
            ml.evaluate("2+2");
            ml.waitForAnswer();

            int result = ml.getInteger();
            System.out.println("2 + 2 = " + result);

            // Here's how to send the same input, but not as a string:
            ml.putFunction("EvaluatePacket", 1);
            ml.putFunction("Plus", 2);
            ml.put(3);
            ml.put(3);
            ml.endPacket();
            ml.waitForAnswer();
            result = ml.getInteger();
            System.out.println("3 + 3 = " + result);

            // If you want the result back as a string, use evaluateToInputForm
            // or evaluateToOutputForm. The second arg for either is the
            // requested page width for formatting the string. Pass 0 for
            // PageWidth->Infinity. These methods get the result in one
            // step--no need to call waitForAnswer.
            String strResult = ml.evaluateToOutputForm("4+4", 0);
            System.out.println("4 + 4 = " + strResult);

        } catch (MathLinkException e) {
            System.out.println("MathLinkException occurred: " + e.getMessage());
        } finally {
            ml.close();
        }
    }
}
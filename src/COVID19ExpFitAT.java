import java.awt.Color;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

import com.github.plot.*;

import com.wolfram.jlink.*;

class FindLogisticFit {
	public static double[] findLogisticFit(long[] cases, String[] args) {
		String empiricData = "{";
		
		for (int i = 0; i < cases.length; i++) {
			empiricData += "{ " + i + ", " + cases[i] + "}";
			if (i < cases.length - 1) {
				empiricData += ", ";
			}
		}
		empiricData += "}";
		
//		System.out.println("Arg: " + arg);
		
		KernelLink ml = null;

        try {
            ml = MathLinkFactory.createKernelLink(args);
        } catch (MathLinkException e) {
            System.out.println("Fatal error opening link: " + e.getMessage());
            return null;
        }
        double[] res = null;
        
        try {
            // Get rid of the initial InputNamePacket the kernel will send
            // when it is launched.
            ml.discardAnswer();

            ml.evaluate("<<MyPackage.m");
            ml.discardAnswer();

            ml.evaluate("EmpiricData = " + empiricData);
            ml.discardAnswer();
            
            ml.evaluate("LogisticModel = a / (1 + b * Exp[-c (x - d)])");
            ml.discardAnswer();
/*            
            String out = ml.evaluateToOutputForm(
            		"LogisticModelFitParameter = FindFit[EmpiricData, LogisticModel, {a, b, c, d}, x]", 0);
            System.out.println("Res: " + out);
*/            
            ml.evaluate("LogisticModelFitParameter = FindFit[EmpiricData, LogisticModel, {a, b, c, d}, x]");
            ml.waitForAnswer();
            
            Expr exp = ml.getExpr();
            
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
            
            double wp = Math.log(b)/c + d;
            
            System.out.println("Wendepunkt: " + wp);
            System.out.println("y(wp) " + a / (1 + b * Math.exp(-c * (wp - d))));
            
            res = new double[]{a, b, c, d};
            
        } catch (MathLinkException e) {
            System.out.println("MathLinkException occurred: " + e.getMessage());
        } finally {
        	ml.close();
        }
        return res;
	}
}

class ExpFit {
	private double _a;
	private double _b;
	private double _error2Sum;

	private ExpFit(double a, double b) {
		this._a = a;
		this._b = b;
	}

	private static double x2y(long[] infactedCases, int max) {
		double result = 0;

		for (int i = 0; i < max; i++) {
			result += i * i * infactedCases[i];
		}

		return result;
	}

	private static double ylny(long[] infactedCases, int max) {
		double result = 0;

		for (int i = 0; i < max; i++) {
			result += infactedCases[i] * Math.log(infactedCases[i]);
		}

		return result;
	}

	private static double xy(long[] infactedCases, int max) {
		double result = 0;

		for (int i = 0; i < max; i++) {
			result += i * infactedCases[i];
		}

		return result;
	}

	private static double xylny(long[] infactedCases, int max) {
		double result = 0;

		for (int i = 0; i < max; i++) {
			result += i * infactedCases[i] * Math.log(infactedCases[i]);
		}

		return result;
	}

	private static double y(long[] infactedCases, int max) {
		double result = 0;

		for (int i = 0; i < max; i++) {
			result += infactedCases[i];
		}

		return result;
	}

	private static double _xy_2(long[] infactedCases, int max) {
		double result = 0;

		for (int i = 0; i < max; i++) {
			result += i * infactedCases[i];
		}
		result = result * result;

		return result;
	}

	private static double a(long[] infactedCases, int max) {
		double result = (x2y(infactedCases, max) * ylny(infactedCases, max)
				- xy(infactedCases, max) * xylny(infactedCases, max))
				/ (y(infactedCases, max) * x2y(infactedCases, max) - _xy_2(infactedCases, max));

		return result;
	}

	private static double b(long[] infactedCases, int max) {
		double result = (y(infactedCases, max) * xylny(infactedCases, max)
				- xy(infactedCases, max) * ylny(infactedCases, max))
				/ (y(infactedCases, max) * x2y(infactedCases, max) - _xy_2(infactedCases, max));

		return result;
	}

	public static ExpFit expFit(long[] cases, int max) {
		ExpFit fit = new ExpFit(ExpFit.a(cases, max), ExpFit.b(cases, max));

		for (int i = 0; i < max; i++) {
			double value = (Math.exp(fit.a()) * Math.exp(fit.b() * i));

			fit._error2Sum += Math.log(Math.abs(cases[i] - value)) * Math.log(Math.abs(cases[i] - value));
		}

		fit._error2Sum /= max;

		return fit;
	}

	// f(t) = Math.exp(a) * Math.exp(b * t);
	public double a() {
		return _a;
	}

	public double b() {
		return _b;
	}

	public double error2() {
		return _error2Sum;
	}

	public double dt() {
		return Math.log(2) / b();
	}

	public Plot.Data fit(long size) {
		Plot.Data fit = Plot.data();

		Calendar cal = Calendar.getInstance();
		cal.set(2020, 1, 25);

		for (int i = 0; i < size; i++) {
			int value = (int) (Math.exp(_a) * Math.exp(_b * i));
			fit.xy(DateWithOffset.getTime(i), value);
			cal.add(Calendar.DAY_OF_MONTH, 1);
		}

		return fit;
	}
	
	public static Plot.Data logisticFit(long size, double a, double b, double c, double d) {
		Plot.Data fit = Plot.data();
		
		Calendar cal = Calendar.getInstance();
		cal.set(2020, 1, 25);
		
		for (int i = 0; i < size; i++) {
			int value = (int) (a / (1 + b * Math.exp(-c * (i - d))));
			fit.xy(DateWithOffset.getTime(i),  value);
			cal.add(Calendar.DAY_OF_MONTH, 1);
		}
		
		return fit;
	}
}

class Infected {
	private static ArrayList<Infected> list = new ArrayList<Infected>();
	private String date;
	private long infectedCount;
	private long testCount;
	private long cummulatedTested;
	
	private static long totalTested = 0;

	private Infected(String date, long infectedCount, long testCount, long totalTested) {
		this.date = date;
		this.infectedCount = infectedCount;
		this.testCount = testCount;
		this.cummulatedTested = totalTested;
	}

	public static void update(String date, long infectedCount, long testCount) {
		totalTested += testCount;
		list.add(new Infected(date, infectedCount, testCount, totalTested));
	}

	public static int numberOfDays() {
		return list.size();
	}

	public static long[] cases() {
		long[] cases = new long[list.size()];
		for (int i = 0; i < list.size(); i++) {
			cases[i] = list.get(i).infectedCount;
		}
		return cases;
	}
	
	public static Iterator<Infected> iterator() {
		return list.iterator();
	}
	
	public long getInfected() {
		return this.infectedCount;
	}
	
	public long getTested() {
		return this.testCount;
	}
	
	public long getCummulatedTested() {
		return this.cummulatedTested;
	}
	
	public String getDate() {
		return this.date;
	}
	
	public static long getTotalTested() {
		return Infected.totalTested;
	}
	
	public static String list() {
		String string = "";
		
		for (int i = 0; i < list.size(); i++) {
			string += "{" + i + "," + list.get(i).infectedCount + "}, ";
		}
		
		return string;
	}
}

class DateWithOffset {
	static long getTime(int offset) {
		return offset;
	}
	
	static long getTimeSave(int offset) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(2020, 1, 25);
		calendar.add(Calendar.DAY_OF_MONTH, offset);
		return calendar.getTimeInMillis();
	}
}

public class COVID19ExpFitAT {
	public static void plot(Plot plot, long[] cases, int numberOfDays, int offset, Color color) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");

		LocalDate localDate = LocalDate.now();
		
		ExpFit expFit = ExpFit.expFit(cases, numberOfDays - offset);
		
		Plot.Data fit = expFit.fit(numberOfDays);
		
		plot.series("Exp-Fit: " + dtf.format(localDate.minusDays(offset)), 
				fit,
				Plot.seriesOpts().
				color(color).
				line(Plot.Line.SOLID).
				marker(Plot.Marker.NONE)
				);
		
		System.out.println("Error: Today - " + offset + " Days: " + Math.sqrt(expFit.error2()));
		System.out.println("Double ratio: Today - " + offset + " Days:  " + expFit.dt());
	}
	
	public static void plotLogistic(Plot plot, int numberOfDays, int offset, Color color, double a, double b, double c, double d) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");

		LocalDate localDate = LocalDate.now();
		
		Plot.Data fit = ExpFit.logisticFit(numberOfDays - offset, a, b, c, d);
		
		plot.series("Log-Fit: " + dtf.format(localDate.minusDays(offset)),
				fit,
				Plot.seriesOpts().
				color(color).
				marker(Plot.Marker.NONE)
				);
	}
	
	public static void main(String[] args) {
		Infected.update("25.2.", 2, 103);
		Infected.update("26.2.", 2, 218);
		Infected.update("27.2.", 3, 124);
		Infected.update("28.2.", 3, 318);
		Infected.update("29.2.", 9, 886);
		Infected.update("1.3.", 14, 177);
		Infected.update("2.3.", 18, 294);
		Infected.update("3.3.", 21, 563);
		Infected.update("4.3.", 29, 455);
		Infected.update("5.3.", 41, 573);
		Infected.update("6.3.", 55, 289);
		Infected.update("7.3.", 79, 308);
		Infected.update("8.3.", 104, 201);
		Infected.update("9.3.", 131, 225);
		Infected.update("10.3.", 182, 292);
		Infected.update("11.3.", 246, 336);
		Infected.update("12.3.", 302, 507);
		Infected.update("13.3.", 504, 713);
		Infected.update("14.3.", 655, 885);
		Infected.update("15.3.", 860, 700);
		Infected.update("16.3.", 1016, 323);
		Infected.update("17.3.", 1332, 1788);
		Infected.update("18.3.", 1646, 1699);
		Infected.update("19.3.", 2053, 1747);
		Infected.update("20.3.", 2388, 1889);
		Infected.update("21.3.", 2814, 2932);
		Infected.update("22.3.", 3244, 2823);
		Infected.update("23.3.", 3924, 2061);
		Infected.update("24.3.", 4876, 4962); // tested total: 28391
		Infected.update("25.3.", 5560, 4016); // tested total: 32407
		Infected.update("26.3.", 6398, 3588); // tested total: 35995
		Infected.update("27.3.", 7399, 3557); // tested total: 39552
		Infected.update("28.3.", 7995, 3198); // tested total: 42750
		Infected.update("29.3.", 8536, 3691); // tested total: 46441
		Infected.update("30.3.", 9377, 3014); // tested total: 49455
		Infected.update("31.3.", 9974, 2889); // tested total: 52344
		Infected.update("1.4.", 10482, 3519); // tested total: 55863
		Infected.update("2.4.", 10967, 36327); // tested total: 92190
		Infected.update("3.4.", 11383, 6153); // tested total: 98343
		Infected.update("4.4.", 11665, 5791); // tested total: 104134 
		Infected.update("5.4.", 11767, 0);

		double[] result = FindLogisticFit.findLogisticFit(Infected.cases(), args);
		
//		System.out.println(Infected.list());
		
		System.out.println("Total Tested: " + Infected.getTotalTested());
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		LocalDate localDate = LocalDate.now();
		localDate = localDate.minusDays(0);
		String dateString = dtf.format(localDate);

		System.out.println(dateString);

		Plot plot = Plot.
				plot(Plot.plotOpts().
						height(700).
						width(1024).
						title("COVID-19 - Least Squares Exponential & Logistic Fit (Austria) - " + dateString).
						legend(Plot.LegendFormat.BOTTOM).
						grids(Infected.numberOfDays() - 1, 10)).
				xAxis("Days", Plot.axisOpts().format(Plot.AxisFormat.NUMBER_INT)).//range(0, 18)).
				yAxis("Infected", Plot.axisOpts().format(Plot.AxisFormat.NUMBER_INT).range(0, 15000));
//				yAxis("Infected", Plot.axisOpts().format(Plot.AxisFormat.NUMBER_INT).range(0, 500000));

		long[] cases = Infected.cases();
		int numberOfDays = Infected.numberOfDays();
		System.out.println("NumDays: " + numberOfDays);

// Fit for today
		COVID19ExpFitAT.plot(plot, cases, numberOfDays, 0, Color.BLUE);

// Fit for today - 1
//		COVID19ExpFitAT.plot(plot, cases, numberOfDays, 22, Color.ORANGE);

// Fit for today - 3
//		COVID19ExpFitAT.plot(plot, cases, numberOfDays, 3, Color.GREEN);
		
// Fit for today - 6
//		COVID19ExpFitAT.plot(plot, cases, numberOfDays, 6, Color.ORANGE);

		COVID19ExpFitAT.plotLogistic(plot, numberOfDays, 0, Color.BLACK, 
				result[0], result[1], result[2], result[3]
				);
		
		Plot.Data asymptote = Plot.data().xy(0, result[0]).xy(numberOfDays - 1, result[0]);
		plot.series("Asymptote (Log-Fit): " + dtf.format(localDate.minusDays(0)),
				asymptote,
				Plot.seriesOpts().
				color(Color.GREEN).
				marker(Plot.Marker.NONE)
				);
/*		
		for (int i = numberOfDays - 5; i >= 0; i--) {

			COVID19ExpFitAT.plot(plot, cases, numberOfDays, i, Color.getHSBColor(1 - (float) i / numberOfDays, 1, 1));
		}
*/

// Empirical data
		Plot.Data empiric = Plot.data();
		for (int i = 0; i < numberOfDays; i++) {
			empiric.xy(DateWithOffset.getTime(i), cases[i]);
		}
		plot.series("Real (3:00 p.m.)", empiric, Plot.seriesOpts().
				color(Color.RED).
				line(Plot.Line.NONE).
				marker(Plot.Marker.CIRCLE));
//				marker(Plot.Marker.NONE));

		
		try {
			plot.save("COVID-19_ExpFit_" + dateString, "png");
		} catch (IOException e) {
			e.printStackTrace();
		}

		
		Plot.Data doubleRatio = Plot.data();

		for (int i = 1; i <= numberOfDays; i++) {
			ExpFit expFit = ExpFit.expFit(cases, i);
			double dt = expFit.dt();

			if (!Double.isNaN(dt) && !Double.isInfinite(dt)) {
				doubleRatio.xy(DateWithOffset.getTime(i - 1), expFit.dt());
			} else {
				doubleRatio.xy(DateWithOffset.getTime(i - 1), 0);
			}
		}

		plot = Plot.
				plot(Plot.plotOpts().height(700).width(1024).
						title("COVID-19 - Least Squares Exponential Fit (Austria) - " + dateString).
						legend(Plot.LegendFormat.BOTTOM).
						grids(Infected.numberOfDays() - 1, 10)).
				xAxis("Days", Plot.axisOpts().format(Plot.AxisFormat.NUMBER_INT)).
				yAxis("Doubling rate", Plot.axisOpts().format(Plot.AxisFormat.NUMBER));

		plot.series("Doubling rate", doubleRatio, Plot.
				seriesOpts().
				color(Color.BLUE).
				marker(Plot.Marker.NONE));

		try {
			plot.save("double-rate", "png");
		} catch (IOException e) {
			e.printStackTrace();
		}

		Plot.Data infected_tested = Plot.data();
		
		Iterator<Infected> it = Infected.iterator();
		
		int i = 0;
		while(it.hasNext()) {
			Infected infected = it.next();
			
			infected_tested.xy(DateWithOffset.getTime(i), 100 * (double) infected.getInfected() / infected.getCummulatedTested());
			i++;
		}
		
		plot = Plot.
				plot(Plot.plotOpts().height(700).width(1024).
						title("COVID-19 - Ratio [%] of Total Infected to Total Tested Persons (Austria) - " + dateString).
						legend(Plot.LegendFormat.BOTTOM).
				grids(Infected.numberOfDays() - 1, 10)).
				xAxis("Days", Plot.axisOpts().format(Plot.AxisFormat.NUMBER_INT)).
				yAxis("Ratio [%]", Plot.axisOpts().format(Plot.AxisFormat.NUMBER));
		
		plot.series("Ratio [%] of total infected to total tested", infected_tested, 
				Plot.seriesOpts().
				color(Color.BLUE).
				marker(Plot.Marker.NONE));

		try {
			plot.save("infected_tested", "png");
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Ready");
	}
}

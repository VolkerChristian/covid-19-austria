import java.awt.Color;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

import com.github.plot.*;

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
		ExpFit fit = new ExpFit(a(cases, max), b(cases, max));

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
		plot.series("Fit: " + dtf.format(localDate.minusDays(offset)), fit,
				Plot.seriesOpts().color(color).marker(Plot.Marker.NONE));
		System.out.println("Error: Today - 2 Days: " + Math.sqrt(expFit.error2()));
		System.out.println("Double ratio: " + expFit.dt());
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
		Infected.update("24.3.", 4876, 4962);

		System.out.println("Total Tested: " + Infected.getTotalTested());
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		LocalDate localDate = LocalDate.now();
		localDate = localDate.minusDays(0);
		String dateString = dtf.format(localDate);

		System.out.println(dateString);

		Plot plot = Plot.
				plot(Plot.plotOpts().height(700).width(1024).
						title("COVID-19 - Least Squares Exponential Fit (Austria) - " + dateString).
						legend(Plot.LegendFormat.BOTTOM).
						grids(Infected.numberOfDays() - 1, 10)).
				xAxis("Days", Plot.axisOpts().format(Plot.AxisFormat.NUMBER_INT)).
				yAxis("Infected", Plot.axisOpts().format(Plot.AxisFormat.NUMBER_INT).range(0, 5000));

		long[] cases = Infected.cases();
		int numberOfDays = Infected.numberOfDays();
		System.out.println("NumDays: " + numberOfDays);

// Fit for today
		COVID19ExpFitAT.plot(plot, cases, numberOfDays, 0, Color.BLUE);

// Fit for today - 1
		COVID19ExpFitAT.plot(plot, cases, numberOfDays, 1, Color.CYAN);

// Fit for today - 3
		COVID19ExpFitAT.plot(plot, cases, numberOfDays, 3, Color.GREEN);
		
// Fit for today - 6
		COVID19ExpFitAT.plot(plot, cases, numberOfDays, 6, Color.ORANGE);

/*		
		for (int i = numberOfDays - 5; i >= 0; i--) {

			COVID19ExpFitAT.plot(plot, cases, numberOfDays, i, Color.GREEN);
		}
*/

// Empirical data
		Plot.Data empiric = Plot.data();
		for (int i = 0; i < numberOfDays; i++) {
			empiric.xy(DateWithOffset.getTime(i), cases[i]);
		}
		plot.series("Real (3:00 p.m.)", empiric, Plot.seriesOpts().color(Color.RED).marker(Plot.Marker.NONE));

		
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
			
			infected_tested.xy(DateWithOffset.getTime(i), (double) infected.getInfected() / infected.getCummulatedTested());
			i++;
		}
		
		plot = Plot.
				plot(Plot.plotOpts().height(700).width(1024).
						title("COVID-19 - Ratio of Total Infected to Total Tested Persons (Austria) - " + dateString).
						legend(Plot.LegendFormat.BOTTOM).
				grids(Infected.numberOfDays() - 1, 10)).
				xAxis("Days", Plot.axisOpts().format(Plot.AxisFormat.NUMBER_INT)).
				yAxis("Ratio", Plot.axisOpts().format(Plot.AxisFormat.NUMBER));
		
		plot.series("Ratio of total infected to total tested", infected_tested, 
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

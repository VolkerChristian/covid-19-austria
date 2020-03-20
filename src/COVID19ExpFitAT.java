import java.awt.Color;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import com.github.plot.*;
import com.github.plot.Plot.Data;

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
		double result = (x2y(infactedCases, max) * ylny(infactedCases, max) - xy(infactedCases, max) * xylny(infactedCases, max))
				/ (y(infactedCases, max) * x2y(infactedCases, max) - _xy_2(infactedCases, max));

		return result;
	}

	private static double b(long[] infactedCases, int max) {
		double result = (y(infactedCases, max) * xylny(infactedCases, max) - xy(infactedCases, max) * ylny(infactedCases, max))
				/ (y(infactedCases, max) * x2y(infactedCases, max) - _xy_2(infactedCases, max));

		return result;
	}

	public static ExpFit expFit(long[] cases, int max) {
		ExpFit fit = new ExpFit(a(cases, max), b(cases, max));

		for (int i = 0; i < max; i++) {
			double value = (Math.exp(fit.a()) * Math.exp(fit.b() * i));

			fit._error2Sum += Math.abs(cases[i] - value) * Math.abs(cases[i] - value);
		}

		fit._error2Sum /= max;

		return fit;
	}

	public double a() {
		return _a;
	}

	public double b() {
		return _b;
	}

	public double error2() {
		return _error2Sum;
	}

	public Data fit(long size) {
		Data d = Plot.data();
		for (int i = 0; i < size; i++) {
			int value = (int) (Math.exp(_a) * Math.exp(_b * i));
			d.xy(i, value);
		}

		return d;
	}
}

class Infected {
	static ArrayList<Infected> list = new ArrayList<Infected>();
	String date;
	long count;

	private Infected(String date, long count) {
		this.date = date;
		this.count = count;
	}

	public static void update(String date, long count) {
		list.add(new Infected(date, count));
	}

	public static int size() {
		return list.size();
	}
	
	public static long[] cases() {
		long[] cases = new long[list.size()];
		for (int i = 0; i < list.size(); i++) {
			cases[i] = list.get(i).count;
		}
		return cases;
	}
}

public class COVID19ExpFitAT {
	public static void main(String[] args) {
		Infected.update("25.2.", 2);
		Infected.update("26.2.", 2);
		Infected.update("27.2.", 3);
		Infected.update("28.2.", 3);
		Infected.update("29.2.", 9);
		Infected.update("1.3.", 14);
		Infected.update("2.3.", 18);
		Infected.update("3.3.", 21);
		Infected.update("4.3.", 29);
		Infected.update("5.3.", 41);
		Infected.update("6.3.", 55);
		Infected.update("7.3.", 79);
		Infected.update("8.3.", 104);
		Infected.update("9.3.", 131);
		Infected.update("10.3.", 182);
		Infected.update("11.3.", 246);
		Infected.update("12.3.", 302);
		Infected.update("13.3.", 504);
		Infected.update("14.3.", 655);
		Infected.update("15.3.", 860);
		Infected.update("16.3.", 1016);
		Infected.update("17.3.", 1332);
		Infected.update("18.3.", 1646);
		Infected.update("19.3.", 2053);
		Infected.update("20.3.", 2388);

		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		LocalDate localDate = LocalDate.now();
		String dateString = dtf.format(localDate);

		System.out.println(dateString);

		Plot plot = Plot.plot(Plot.plotOpts().height(700).width(1024)
				.title("COVID-19 - Least Squares Exponential Fit (Austria) - " + dateString)
				.legend(Plot.LegendFormat.BOTTOM)).xAxis("Days", null).yAxis("Infected", null);

		long[] cases = Infected.cases();
		int size = Infected.size();

// Empirical data
		Data d1 = Plot.data();
		for (int i = 0; i < size; i++) {
			d1.xy(i, cases[i]);
		}
		plot.series("Real (3:00 p.m.)", d1, 
				Plot.seriesOpts().color(Color.RED).marker(Plot.Marker.NONE));
		
// Fit for today
		ExpFit expFit = ExpFit.expFit(cases, size);
		Data d = expFit.fit(size);
		plot.series("Fit: " + dtf.format(localDate), d, 
				Plot.seriesOpts().color(Color.BLUE).marker(Plot.Marker.NONE));
		System.out.println("Error: Today - 0 Days: " + Math.sqrt(expFit.error2()));

// Fit for today - 1
		expFit = ExpFit.expFit(cases, size - 1);
		d = expFit.fit(size);
		plot.series("Fit: " + dtf.format(localDate.minusDays(1)), d,
				Plot.seriesOpts().color(Color.CYAN).marker(Plot.Marker.NONE));
		System.out.println("Error: Today - 2 Days: " + Math.sqrt(expFit.error2()));

// Fit for today - 3
		expFit = ExpFit.expFit(cases, size - 3);
		d = expFit.fit(size);
		plot.series("Fit: " + dtf.format(localDate.minusDays(3)), d,
				Plot.seriesOpts().color(Color.GREEN).marker(Plot.Marker.NONE));
		System.out.println("Error: Today - 2 Days: " + Math.sqrt(expFit.error2()));

// Fit for today - 6
		expFit = ExpFit.expFit(cases, size - 6);
		d = expFit.fit(size);
		plot.series("Fit: " + dtf.format(localDate.minusDays(6)), d,
				Plot.seriesOpts().color(Color.ORANGE).marker(Plot.Marker.NONE));
		System.out.println("Error: Today - 5 Days: " + Math.sqrt(expFit.error2()));

		try {
			plot.save("COVID-19_ExpFit_" + dateString, "png");
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Ready");
	}
}

import java.awt.Color;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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

	private static double x2y(double[] array, int max) {
		double result = 0;

		for (int i = 0; i < max; i++) {
			result += i * i * array[i];
		}

		return result;
	}

	private static double ylny(double[] array, int max) {
		double result = 0;

		for (int i = 0; i < max; i++) {
			result += array[i] * Math.log(array[i]);
		}

		return result;
	}

	private static double xy(double[] array, int max) {
		double result = 0;

		for (int i = 0; i < max; i++) {
			result += i * array[i];
		}

		return result;
	}

	private static double xylny(double[] array, int max) {
		double result = 0;

		for (int i = 0; i < max; i++) {
			result += i * array[i] * Math.log(array[i]);
		}

		return result;
	}

	private static double y(double[] array, int max) {
		double result = 0;

		for (int i = 0; i < max; i++) {
			result += array[i];
		}

		return result;
	}

	private static double _xy_2(double[] array, int max) {
		double result = 0;

		for (int i = 0; i < max; i++) {
			result += i * array[i];
		}
		result = result * result;

		return result;
	}

	private static double a(double[] array, int max) {
		double result = (x2y(array, max) * ylny(array, max) - xy(array, max) * xylny(array, max))
				/ (y(array, max) * x2y(array, max) - _xy_2(array, max));

		return result;
	}

	private static double b(double[] array, int max) {
		double result = (y(array, max) * xylny(array, max) - xy(array, max) * ylny(array, max))
				/ (y(array, max) * x2y(array, max) - _xy_2(array, max));

		return result;
	}

	public static ExpFit expFit(double[] array, int max) {
		ExpFit fit = new ExpFit(a(array, max), b(array, max));

		for (int i = 0; i < max; i++) {
			double value = (Math.exp(fit.a()) * Math.exp(fit.b() * i));

			fit._error2Sum += Math.abs(array[i] - value) * Math.abs(array[i] - value);
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

	public Data fit(int days) {
		Data d = Plot.data();
		for (int i = 0; i < days; i++) {
			int value = (int) (Math.exp(_a) * Math.exp(_b * i));
			d.xy(i, value);
		}

		return d;
	}
}

public class COVID19ExpFitAT {
	public static void main(String[] args) {
		double[] array = { 2, 2, 3, 3, 9, 14, 18, 21, 29, 41, 55, 79, 
				104, 131, 182, 246, 302, 504, 655, 860, 1016, 1332, 1646, 2053 };

		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		LocalDate localDate = LocalDate.now();
		String dateString = dtf.format(localDate);
		
		System.out.println(dateString);
		
		Plot plot = Plot.plot(Plot.plotOpts().height(700).width(1024).
				title("COVID-19 - Least Squares Exponential Fit (Austria) - " + dateString).
				legend(Plot.LegendFormat.BOTTOM)).
				xAxis("Days", null).
				yAxis("Infected", null);
		
		Data d1 = Plot.data();
		for (int i = 0; i < array.length; i++) {
			d1.xy(i, array[i]);
		}
		plot.series("Real (3:00 p.m.)", d1, Plot.seriesOpts().color(Color.RED).marker(Plot.Marker.NONE));

		ExpFit expFit = ExpFit.expFit(array, array.length);
		Data d = expFit.fit(array.length);
		plot.series("Fit: " + dtf.format(localDate), d, Plot.seriesOpts().color(Color.BLUE).marker(Plot.Marker.NONE));
		System.out.println("Error: Today - 0 Days: " + Math.sqrt(expFit.error2()));
		
		expFit = ExpFit.expFit(array, array.length - 2);
		d = expFit.fit(array.length);
		plot.series("Fit: " + dtf.format(localDate.minusDays(2)), d, Plot.seriesOpts().color(Color.GREEN).marker(Plot.Marker.NONE));
		System.out.println("Error: Today - 2 Days: " + Math.sqrt(expFit.error2()));

		expFit = ExpFit.expFit(array, array.length - 5);
		d = expFit.fit(array.length);
		plot.series("Fit: " + dtf.format(localDate.minusDays(5)), d, Plot.seriesOpts().color(Color.ORANGE).marker(Plot.Marker.NONE));
		System.out.println("Error: Today - 5 Days: " + Math.sqrt(expFit.error2()));
/*	
		expFit = ExpFit.expFit(array, array.length - 10);
		d = expFit.fit(array.length);
		plot.series("Fit: " + dtf.format(localDate.minusDays(10)), d, Plot.seriesOpts().color(Color.BLACK).marker(Plot.Marker.NONE));
		System.out.println("Error: Today - 10 Days: " + Math.sqrt(expFit.error2()));
*/
		try {
			plot.save("COVID-19_ExpFit_" + dateString, "png");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Ready ");
	}
}

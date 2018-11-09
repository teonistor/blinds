package io.github.teonistor.blindshm;

import javax.swing.*;

import com.amd.aparapi.Kernel;

import java.awt.*;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.*;
import static java.util.stream.IntStream.range;
import static java.lang.Math.atan;
import static java.lang.Math.max;

public class App extends JPanel {
	private static final long serialVersionUID = 6525603291529393737L;
	
	private double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
	private Set<Double> distinctValues = new TreeSet<>();
	private int draw = 0;

//	static final int N = 50;
	private final AtomicReference<Double>
			d = new AtomicReference<>(13.0),
			w = new AtomicReference<>(30.0);
	
	private final AtomicReference<Integer> N = new AtomicReference<>(50);

	public static void main (final String arg[]) {
		System.out.println(System.getProperties().get("java.library.path"));
//		System.loadLibrary("io_github_teonistor_blindshm_App.so");

		MyKernel mk = new MyKernel();
		mk.execute(5);
		
		System.out.println("Running v0.10");
		
		new App();
		
		
	}
	
	private native int nativeAdd(int a, int b);


	App() {
		final JFrame frame = new JFrame("J Heat map experiment");
		frame.setLayout(new BorderLayout());
		
		final JPanel controls = new JPanel(new FlowLayout());
		Stream.of(
				new UIBinding<>("N=", N, Integer::valueOf),
				new UIBinding<>("d=", d, Double::valueOf),
				new UIBinding<>("w=", w, Double::valueOf))
			.flatMap(UIBinding::streamComponents)
			.forEach(controls::add);

		
		
		setMinimumSize(new Dimension(800, 600));
		setPreferredSize(getMinimumSize());

		frame.add(controls, BorderLayout.NORTH);
		frame.add(this, BorderLayout.CENTER);
		
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setVisible(true);
		frame.setMinimumSize(frame.getPreferredSize());

		
//		System.out.printf("Test nativeAdd: %d + %d = %d%n", 3, 7, nativeAdd(3, 7));
	}

	@Override public void repaint() {
		// TODO
	}

	@Override public void paint(final Graphics g) {
		super.paint(g);
		System.out.printf("Draw %d... ", ++draw);

		AtomicReference<Double> localMin = new AtomicReference<Double>(min);
		AtomicReference<Double> localMax = new AtomicReference<Double>(max);

//		double min = distinctValues.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
//		double max = distinctValues.stream().mapToDouble(Double::doubleValue).max().orElse(10000.0);
		
		magicRange(getWidth(), D -> {
			magicRange(getHeight(), h -> {
				double sum = magicRange(N, (IntToDoubleFunction) n -> alpha(d.get(), D, w.get(), h, n)).sum();

				if (draw > 1) {
					float f = (float) ((sum - min) / (max - min));
					if (f >= 1f) f = 1f;
					else if (f <= 0f) f = 0f;
	
					g.setColor(new Color(f*f, f*f, f*f));
					g.fillRect(D, h, 1, 1);
				}

				localMin.accumulateAndGet(sum, Math::min);
				localMax.accumulateAndGet(sum, Math::max);
				distinctValues.add(sum);
			});
		});

		min = localMin.get();
		max = localMax.get();
		System.out.printf("Min = %f, max = %f%n", min, max);
//		System.out.printf("Draw %d: %s distinct values%n", ++draw, distinctValues.size());
	}
	
	private void magicRange(int upperLimitIncl, IntConsumer consumer) {
		range(0, upperLimitIncl + 1).forEach(consumer);
	}
	
	private DoubleStream magicRange(AtomicReference<Integer> upperLimitIncl, IntToDoubleFunction function) {
		return range(0, upperLimitIncl.get() + 1).mapToDouble(function);
	}

	private double alpha(double d, double D, double w, double h, int n) {
		if ((n-1) * d > h) return alpha1(d, D, w, h, n);
		else if (n * d < h) return alpha2(d, D, w, h, n);
		else return alpha3(d, D, w, h, n);
	}
	
	private double alpha1(double d, double D, double w, double h, int n) {
		return max(atan((n*d-h) / (w+D)) - atan((n*d-d-h) / (D)), 0.0);
	}

	private double alpha2(double d, double D, double w, double h, int n) {
		return max(atan((h-n*d+d) / (w+D)) - atan((h-n*d) / (D)), 0.0);
	}

	private double alpha3(double d, double D, double w, double h, int n) {
		return atan((h-n*d+d) / (w+D)) + atan((n*d-h) / (w+D));
	}

	static class MyKernel extends Kernel {

		@Override public void run() {
			System.out.print(getGlobalId());
			new RuntimeException("test").printStackTrace();
		}
		
	}
	
	class UIBinding <T> {
		static final int fieldWidth = 6;
		
		final JLabel label;
		final JTextField field;
		
		public UIBinding(String label, AtomicReference<T> target, Function<String, T> mapper) {
			this.label = new JLabel(label);
			this.field = new JTextField(target.get().toString(), fieldWidth);
			field.addActionListener(e -> {
				try {
					target.set(mapper.apply(field.getText()));
				} catch (final Exception ex) {
					ex.printStackTrace();
				}
			});
			
			repaint();
		}

		public Stream<JComponent> streamComponents() {
			return Stream.of(label, field);
		}
	}
}
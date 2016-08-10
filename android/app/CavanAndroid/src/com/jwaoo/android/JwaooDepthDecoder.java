package com.jwaoo.android;

import com.cavan.java.CavanSquareWaveCounter;

@SuppressWarnings("serial")
public class JwaooDepthDecoder extends CavanSquareWaveCounter {

	public static final long TIME_MIN = 800;
	public static final long TIME_MAX = 2000;

	private int mCount;
	private double mDepth;
	private double mDepthAlign;
	private boolean mPlugIn = true;

	private JwaooDepthSquareWaveGenerator mGenerators[] = new JwaooDepthSquareWaveGenerator[JwaooToySensor.SENSOR_COUNT];

	public JwaooDepthDecoder(double fuzz) {
		super(fuzz, TIME_MIN, TIME_MAX);

		for (int i = 0; i < JwaooToySensor.SENSOR_COUNT; i++) {
			mGenerators[i] = new JwaooDepthSquareWaveGenerator(fuzz, TIME_MIN, TIME_MAX);
		}
	}

	@Override
	public void setValueFuzz(double fuzz) {
		super.setValueFuzz(fuzz);

		for (int i = 0; i < JwaooToySensor.SENSOR_COUNT; i++) {
			mGenerators[i].setValueFuzz(fuzz);
		}
	}

	@Override
	public void setTimeMin(long time) {
		super.setTimeMin(time);

		for (int i = 0; i < JwaooToySensor.SENSOR_COUNT; i++) {
			mGenerators[i].setTimeMin(time);
		}
	}

	public void setTimeMax(long time) {
		if (time < TIME_MIN) {
			time = TIME_MIN;
		}

		super.setOverTime(time);

		for (int i = 0; i < JwaooToySensor.SENSOR_COUNT; i++) {
			mGenerators[i].setOverTime(time);
		}
	}

	@Override
	protected void updateThreshold(double min, double max) {
		if (mValueRange > mValueFuzz) {
			mThresholdHigh = (min + max + mValueFuzz) / 2;
			mThresholdLow = mThresholdHigh - mValueFuzz;
		}
	}

	public double getDepth() {
		return mDepth;
	}

	public JwaooDepthSquareWaveGenerator[] getGenerators() {
		return mGenerators;
	}

	public JwaooDepthSquareWaveGenerator getGenerator(int index) {
		return mGenerators[index];
	}

	public double calculateDepth(int count) {
		if (count < JwaooToySensor.SENSOR_COUNT) {
			if (count > 0) {
				return 0.5 + count - 1;
			} else {
				return 0;
			}
		} else {
			return JwaooToySensor.SENSOR_COUNT;
		}
	}

	public double calculatePredictedDepth(int count) {
		double depth = mGenerators[count - 1].getPredictedValue() + mGenerators[count].getPredictedValue() - 0.5;

		if (depth > 1) {
			return 1;
		}

		if (depth < 0) {
			return 0;
		}

		return depth;
	}

	public double putCapacityValue(double[] capacitys) {
		double sum = 0;

		for (int i = capacitys.length - 1; i >= 0; i--) {
			sum += capacitys[i];
		}

		double freq = putFreqValue(sum);
		if (freq > 0) {
			setTimeMax((long) (2000 / freq));
		} else {
			setTimeMax(TIME_MAX);
		}

		int count = 0;

		for (int i = 0; i < JwaooToySensor.SENSOR_COUNT; i++) {
			if (mGenerators[i].putValue(capacitys[i])) {
				count = i + 1;
			}
		}

		// CavanAndroid.logE("count = " + count);

		if (count > mCount || count <= 0) {
			mPlugIn = true;
			mDepth = mDepthAlign = calculateDepth(count);
		} else if (count < mCount) {
			mPlugIn = false;
			mDepth = mDepthAlign = calculateDepth(count);
		} else {
			if (mPlugIn) {
				for (int i = count + 1; i < JwaooToySensor.SENSOR_COUNT; i++) {
					mGenerators[i].savePredictedMin();
				}

				for (int i = count - 2; i >= 0; i--) {
					mGenerators[i].savePredictMax();
				}
			}

			if (count < JwaooToySensor.SENSOR_COUNT) {
				if (count > 0) {
					mDepth = calculatePredictedDepth(count) + mDepthAlign;
				} else {
					mDepth = 0.0;
				}
			} else {
				mDepth = 1.0;
			}
		}

		mCount = count;

		// CavanAndroid.logE("depth = " + mDepth);

		return mDepth;
	}
}

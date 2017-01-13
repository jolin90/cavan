package com.cavan.java;

import java.security.InvalidParameterException;

public class VoltageCapacityTable {

	public static class Entry {

		private int mCapacity;
		private float mVoltage;

		public Entry(float voltage, int capacity) {
			mVoltage = voltage;
			mCapacity = capacity;
		}

		public int getCapacity() {
			return mCapacity;
		}

		public void setCapacity(int capacity) {
			mCapacity = capacity;
		}

		public float getVoltage() {
			return mVoltage;
		}

		public void setVoltage(float voltage) {
			mVoltage = voltage;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();

			builder.append('{').append(mVoltage).append(", ").append(mCapacity).append('}');

			return builder.toString();
		}
	}

	public static void main(String args[]) {
		Entry[] entries = {
			new Entry(3200, 0),
			new Entry(3400, 10),
			new Entry(3700, 80),
			new Entry(4200, 100),
		};

		VoltageCapacityTable table = new VoltageCapacityTable(entries);

		for (int voltage = 3000; voltage < 4300; voltage += 10) {
			CavanJava.dLog("voltage = " + voltage + ", capacity = " + table.getCapacity(voltage));
		}
	}

	private Entry[] mEntries;

	public VoltageCapacityTable(Entry[] entries) {
		if (entries != null && entries.length > 0) {
			mEntries = entries;
		} else {
			throw new InvalidParameterException("Invalid entries = " + entries);
		}
	}

	public VoltageCapacityTable(float minVoltage, float maxVoltage, int maxCapacity) {
		mEntries = new Entry[] {
			new Entry(minVoltage, 0),
			new Entry(maxVoltage, maxCapacity)
		};
	}

	public VoltageCapacityTable(float minVoltage, float maxVoltage) {
		this(minVoltage, maxVoltage, 100);
	}

	public Entry[] getEntries() {
		return mEntries;
	}

	public int findEntryByVoltage(float voltage, int start, int end) {
		if (start == end) {
			return start;
		}

		int i = (start + end) / 2;
		if (voltage > mEntries[i].getVoltage()) {
			start = i + 1;
		} else {
			end = i;
		}

		return findEntryByVoltage(voltage, start, end);
	}

	public float getCapacity(float voltage) {
		int end = mEntries.length - 1;
		int index = findEntryByVoltage(voltage, 0, end);

		int minCapacity, maxCapacity;
		float minVoltage, maxVoltage;

		Entry entry = mEntries[index];

		if (voltage < entry.getVoltage()) {
			if (index > 0) {
				Entry prev = mEntries[index - 1];

				minVoltage = prev.getVoltage();
				minCapacity = prev.getCapacity();
			} else {
				return entry.getCapacity();
			}

			maxVoltage = entry.getVoltage();
			maxCapacity = entry.getCapacity();
		} else {
			if (index < end) {
				Entry next = mEntries[index + 1];

				maxVoltage = next.getVoltage();
				maxCapacity = next.getCapacity();
			} else {
				return entry.getCapacity();
			}

			minVoltage = entry.getVoltage();
			minCapacity = entry.getCapacity();
		}

		return (voltage - minVoltage) * (maxCapacity - minCapacity) / (maxVoltage - minVoltage) + minCapacity;
	}

	public int getCapacityInt(float voltage) {
		return (int) Math.round(getCapacity(voltage));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append("{\n");

		for (Entry entry : mEntries) {
			builder.append("  ").append(entry).append(",\n");
		}

		builder.append("}\n");

		return builder.toString();
	}
}

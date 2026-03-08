package ch.njol.skript.core.lang;

import ch.njol.skript.core.event.EventValues;
import ch.njol.skript.core.variables.VariableRef;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Expression: [amount] random number(s) | integer(s) between X and Y.
 * Returns null for null/NaN/infinity bounds, amount <= 0, or integer range with no whole numbers.
 */
public final class ExprRandomNumber implements Expression<Object> {

	private final Object amountParsed;  // null = "a" (1), else Number or VariableRef
	private final Object lowerParsed;
	private final Object upperParsed;
	private final boolean isInteger;

	public ExprRandomNumber(Object amountParsed, Object lowerParsed, Object upperParsed, boolean isInteger) {
		this.amountParsed = amountParsed;
		this.lowerParsed = lowerParsed;
		this.upperParsed = upperParsed;
		this.isInteger = isInteger;
	}

	@Override
	public Object get(ExecutionContext ctx) {
		Number lowerNumber = resolveNumber(lowerParsed, ctx);
		Number upperNumber = resolveNumber(upperParsed, ctx);
		if (lowerNumber == null || upperNumber == null ||
			!Double.isFinite(lowerNumber.doubleValue()) || !Double.isFinite(upperNumber.doubleValue())) {
			return null;
		}

		int amount = 1;
		if (amountParsed != null) {
			Object a = amountParsed instanceof Expression<?> ex ? ex.get(ctx) : EventValues.resolve(amountParsed, ctx);
			if (a == null) return null;
			if (a instanceof Number n) {
				double d = n.doubleValue();
				if (Double.isNaN(d) || !Double.isFinite(d) || d <= 0) return null;
				amount = n.intValue();
				if (amount <= 0) return null;
			} else {
				return null;
			}
		}

		double lo = Math.min(lowerNumber.doubleValue(), upperNumber.doubleValue());
		double hi = Math.max(lowerNumber.doubleValue(), upperNumber.doubleValue());
		var random = ThreadLocalRandom.current();

		// When range is a single point, return that value (deterministic; fixes "0 and 0" -> 0)
		if (lo == hi) {
			if (isInteger) {
				long v = (long) Math.floor(lo);
				if (amount == 1) return Long.valueOf(v);
				List<Number> list = new ArrayList<>(amount);
				for (int i = 0; i < amount; i++) list.add(Long.valueOf(v));
				return list;
			}
			double v = lo;
			// Use Long when whole number so "is 0" assertion matches (e.g. 0 and 0)
			Number single = (v == Math.rint(v)) ? Long.valueOf((long) v) : Double.valueOf(v);
			if (amount == 1) return single;
			List<Number> list = new ArrayList<>(amount);
			for (int i = 0; i < amount; i++) list.add(single);
			return list;
		}

		if (isInteger) {
			long floorHi = (long) Math.floor(hi);
			long ceilLo = (long) Math.ceil(lo);
			// No integer in range (e.g. 0.5 to 0.6)
			if (ceilLo > floorHi) return null;
			// Single integer in range
			if (ceilLo == floorHi) {
				if (amount == 1) return Long.valueOf(ceilLo);
				List<Number> list = new ArrayList<>(amount);
				for (int i = 0; i < amount; i++) list.add(Long.valueOf(ceilLo));
				return list;
			}
			long range = floorHi - ceilLo + 1;
			if (amount == 1) {
				long v = ceilLo + mod(random.nextLong(), range);
				return Long.valueOf(v);
			}
			List<Number> list = new ArrayList<>(amount);
			for (int i = 0; i < amount; i++)
				list.add(Long.valueOf(ceilLo + mod(random.nextLong(), range)));
			return list;
		}

		// non-integer
		if (amount == 1) {
			double v = Math.min(lo + random.nextDouble() * (hi - lo), hi);
			return Double.valueOf(v);
		}
		List<Number> list = new ArrayList<>(amount);
		for (int i = 0; i < amount; i++)
			list.add(Double.valueOf(Math.min(lo + random.nextDouble() * (hi - lo), hi)));
		return list;
	}

	private static Number resolveNumber(Object parsed, ExecutionContext ctx) {
		Object v;
		if (parsed instanceof Expression<?> ex) {
			v = ex.get(ctx);
		} else {
			v = EventValues.resolve(parsed, ctx);
		}
		if (v instanceof Number n) return n;
		if (v == null) return null;
		if (v instanceof String s) {
			try {
				if (s.contains(".")) return Double.parseDouble(s);
				return Long.parseLong(s);
			} catch (NumberFormatException ignored) {
				return null;
			}
		}
		return null;
	}

	private static long mod(long x, long m) {
		if (m <= 0) return 0;
		long r = x % m;
		return r < 0 ? r + m : r;
	}
}

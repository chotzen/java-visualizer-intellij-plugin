package com.aegamesi.java_visualizer.model;

import org.json.JSONArray;

public class Value {
	// primitive or reference
	public boolean isArg = false;
	public Type type;
	public long longValue;
	public double doubleValue;
	public boolean booleanValue;
	public String stringValue;
	public char charValue;
	public String codeValue;
	public long reference;
	public String referenceType;
	public Frame holeDest;
	public String holeString;
	public long hashCode;
	public int scannerPos;

	@Override
	public String toString() {
		switch (type) {
			case NULL:
				return "null";
			case STRING:
				return "\"" + stringValue + "\"";
			case CODE:
				return codeValue;
			case TUPLE:
			case SCANNER_BLOB:
				return stringValue;
			case LONG:
				return Long.toString(longValue);
			case DOUBLE:
				return Double.toString(doubleValue);
			case BOOLEAN:
				return Boolean.toString(booleanValue);
			case CHAR:
				return "'" + charValue + "'";
			case REFERENCE:
				return "*REF*";
			case HOLE:
				return "*HOLE*";
			default:
				return "<?>";
		}
	}

	public enum Type {
		NULL, VOID, LONG, DOUBLE, BOOLEAN, STRING, CHAR, REFERENCE, HOLE, CODE, SCANNER_BLOB, END_OF_VISUALIZATION, TUPLE;
	}

	JSONArray toJson() {
		JSONArray a = new JSONArray();
		a.put(type.name());
		switch (type) {
			case STRING:
				a.put(stringValue);
				break;
			case LONG:
				a.put(longValue);
				break;
			case DOUBLE:
				a.put(doubleValue);
				break;
			case BOOLEAN:
				a.put(booleanValue);
				break;
			case CHAR:
				a.put(charValue);
				break;
			case REFERENCE:
				a.put(reference);
				break;
		}
		return a;
	}

	static Value fromJson(JSONArray a) {
		Value v = new Value();
		v.type = Type.valueOf(a.getString(0));
		switch (v.type) {
			case STRING:
				v.stringValue = a.getString(1);
				break;
			case LONG:
				v.longValue = a.getLong(1);
				break;
			case DOUBLE:
				v.doubleValue = a.getDouble(1);
				break;
			case BOOLEAN:
				v.booleanValue = a.getBoolean(1);
				break;
			case CHAR:
				v.charValue = (char) a.getInt(1);
				break;
			case REFERENCE:
				v.reference = a.getLong(1);
				break;
		}
		return v;
	}
}

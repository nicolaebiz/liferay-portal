/**
 * Copyright (c) 2000-2006 Liferay, Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portal.upgrade.util;

import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.spring.hibernate.HibernateUtil;
import com.liferay.portal.upgrade.UpgradeException;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.DateUtil;
import com.liferay.util.FileUtil;
import com.liferay.util.GetterUtil;
import com.liferay.util.StringUtil;
import com.liferay.util.dao.DataAccess;
import com.liferay.util.dao.hibernate.BooleanType;
import com.liferay.util.dao.hibernate.FloatType;
import com.liferay.util.dao.hibernate.IntegerType;
import com.liferay.util.dao.hibernate.LongType;
import com.liferay.util.dao.hibernate.ShortType;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;

import java.text.DateFormat;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.usertype.UserType;

/**
 * <a href="BaseUpgradeTableImpl.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Alexander Chow
 * @author  Brian Wing Shun Chan
 *
 */
public abstract class BaseUpgradeTableImpl {

	public BaseUpgradeTableImpl(String tableName, Object[][] columns) {
		_tableName = tableName;
		_columns = columns;
	}

	public String getTableName() {
		return _tableName;
	}

	public Object[][] getColumns() {
		return _columns;
	}

	public abstract String getExportedData(ResultSet rs) throws Exception;

	public void appendColumn(StringBuffer sb, Object value, boolean last)
		throws Exception {

		if (value == null) {
			throw new UpgradeException(
				"Nulls should never be inserted into the database");
		}
		else if (value instanceof String) {
			sb.append(
				StringUtil.replace(
					(String)value, StringPool.COMMA, _SAFE_COMMA_CHARACTER));
		}
		else if (value instanceof Date) {
			DateFormat df = DateUtil.getISOFormat();

			sb.append(df.format(value));
		}
		else {
			sb.append(value);
		}

		if (last) {
			sb.append(StringPool.NEW_LINE);
		}
		else {
			sb.append(StringPool.COMMA);
		}
	}

	public void appendColumn(
			StringBuffer sb, ResultSet rs, String name, Integer type,
			boolean last)
		throws Exception {

		Object value = getValue(rs, name, type);

		appendColumn(sb, value, last);
	}

	public String getDeleteSQL() throws Exception {
		return "DELETE FROM " + _tableName;
	}

	public String getInsertSQL() throws Exception {
		String sql = "INSERT INTO " + _tableName + " (";

		for (int i = 0; i < _columns.length; i++) {
			sql += _columns[i][0];

			if ((i + 1) < _columns.length) {
				sql += ", ";
			}
			else {
				sql += ") VALUES (";
			}
		}

		for (int i = 0; i < _columns.length; i++) {
			sql += "?";

			if ((i + 1) < _columns.length) {
				sql += ", ";
			}
			else {
				sql += ")";
			}
		}

		return sql;
	}

	public String getSelectSQL() throws Exception {
		String sql = "SELECT ";

		for (int i = 0; i < _columns.length; i++) {
			sql += _columns[i][0];

			if ((i + 1) < _columns.length) {
				sql += ", ";
			}
			else {
				sql += " FROM " + _tableName;
			}
		}

		return sql;
	}

	public Object getValue(ResultSet rs, String name, Integer type)
		throws Exception {

		Object value = null;

		int t = type.intValue();

		UserType userType = null;

		if (t == Types.BIGINT) {
			userType = new LongType();
		}
		else if (t == Types.BOOLEAN) {
			userType = new BooleanType();
		}
		else if (t == Types.TIMESTAMP) {
			try {
				value = rs.getObject(name);
			}
			catch (Exception e) {
			}

			if (value == null) {
				value = new Date();
			}
		}
		else if (t == Types.FLOAT) {
			userType = new FloatType();
		}
		else if (t == Types.INTEGER) {
			userType = new IntegerType();
		}
		else if (t == Types.SMALLINT) {
			userType = new ShortType();
		}
		else if (t == Types.VARCHAR) {
			value = GetterUtil.getString(rs.getString(name));
		}
		else {
			throw new UpgradeException(
				"Upgrade code using unsupported class type " + type);
		}

		if (userType != null) {
			value = userType.nullSafeGet(rs, new String[] {name}, null);
		}

		return value;
	}

	public void setColumn(
			PreparedStatement ps, int index, Integer type, String value)
		throws Exception {

		int t = type.intValue();

		if (t == Types.BIGINT) {
			ps.setLong(index, GetterUtil.getLong(value));
		}
		else if (t == Types.BOOLEAN) {
			ps.setBoolean(index, GetterUtil.getBoolean(value));
		}
		else if (t == Types.TIMESTAMP) {
			DateFormat df = DateUtil.getISOFormat();

			ps.setTimestamp(
				index, new Timestamp(df.parse(value).getTime()));
		}
		else if (t == Types.FLOAT) {
			ps.setFloat(index, GetterUtil.getFloat(value));
		}
		else if (t == Types.INTEGER) {
			ps.setInt(index, GetterUtil.getInteger(value));
		}
		else if (t == Types.SMALLINT) {
			ps.setShort(index, GetterUtil.getShort(value));
		}
		else if (t == Types.VARCHAR) {
			value =
				StringUtil.replace(
					value, _SAFE_COMMA_CHARACTER, StringPool.COMMA);

			ps.setString(index, value);
		}
		else {
			throw new UpgradeException(
				"Upgrade code using unsupported class type " + type);
		}
	}

	public void updateTable() throws Exception {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		boolean isEmpty = true;

		String tempFilename =
			"temp-db-" + _tableName + "-" + System.currentTimeMillis();

		String selectSQL = getSelectSQL();

		BufferedWriter bw = new BufferedWriter(new FileWriter(tempFilename));

		try {
			con = HibernateUtil.getConnection();

			ps = con.prepareStatement(selectSQL);

			rs = ps.executeQuery();

			while (rs.next()) {
				bw.write(getExportedData(rs));

				isEmpty = false;
			}

			_log.info(_tableName + " table backed up to file " + tempFilename);
		}
		finally {
			DataAccess.cleanUp(con, ps, rs);

			bw.close();
		}

		if (!isEmpty) {
			Statement stmt = null;

			try {
				con = HibernateUtil.getConnection();

				stmt = con.createStatement();

				stmt.executeUpdate(getDeleteSQL());
			}
			finally {
				DataAccess.cleanUp(con, stmt);
			}

			String insertSQL = getInsertSQL();

			BufferedReader br = new BufferedReader(
				new FileReader(tempFilename));

			String line = null;

			try {
				con = HibernateUtil.getConnection();

				int count = 0;

				while ((line = br.readLine()) != null) {
					String[] values = StringUtil.split(line);

					if (values.length != _columns.length) {
						throw new UpgradeException(
							"Columns differ between temp file and schema");
					}

					if (count == 0) {
						ps = con.prepareStatement(insertSQL);
					}

					for (int i = 0; i < values.length; i++) {
						setColumn(
							ps, i + 1, (Integer)_columns[i][1], values[i]);
					}

					ps.addBatch();

					if (count == _BATCH_SIZE) {
						ps.executeBatch();

						ps.close();

						count = 0;
					}
					else {
						count++;
					}
				}

				if (count != 0) {
					ps.executeBatch();

					ps.close();
				}
			}
			finally {
				DataAccess.cleanUp(con, ps);

				br.close();
			}
		}

		FileUtil.delete(tempFilename);

		_log.info(_tableName + " table repopulated with data");
	}

	private static final int _BATCH_SIZE = GetterUtil.getInteger(
		PropsUtil.get("hibernate.jdbc.batch_size"));

	private static final String _SAFE_COMMA_CHARACTER =
		"_SAFE_COMMA_CHARACTER_";

	private static Log _log = LogFactory.getLog(BaseUpgradeTableImpl.class);

	private String _tableName;
	private Object[][] _columns;

}
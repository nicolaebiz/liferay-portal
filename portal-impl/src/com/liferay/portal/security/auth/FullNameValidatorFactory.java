/**
 * Copyright (c) 2000-2012 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.security.auth;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ClassUtil;
import com.liferay.portal.kernel.util.InstanceFactory;
import com.liferay.portal.security.pacl.PACLClassLoaderUtil;
import com.liferay.portal.util.PropsValues;

/**
 * @author Amos Fong
 */
public class FullNameValidatorFactory {

	public static FullNameValidator getInstance() {
		if (_originalFullNameValidator == null) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"Instantiate " + PropsValues.USERS_FULL_NAME_VALIDATOR);
			}

			ClassLoader classLoader =
				PACLClassLoaderUtil.getPortalClassLoader();

			try {
				_originalFullNameValidator =
					(FullNameValidator)InstanceFactory.newInstance(
						classLoader, PropsValues.USERS_FULL_NAME_VALIDATOR);
			}
			catch (Exception e) {
				_log.error(e, e);
			}
		}

		if (_fullNameValidator == null) {
			_fullNameValidator = _originalFullNameValidator;
		}

		if (_log.isDebugEnabled()) {
			_log.debug("Return " + ClassUtil.getClassName(_fullNameValidator));
		}

		return _fullNameValidator;
	}

	public static void setInstance(FullNameValidator fullNameValidator) {
		if (_log.isDebugEnabled()) {
			_log.debug("Set " + ClassUtil.getClassName(fullNameValidator));
		}

		if (fullNameValidator == null) {
			_fullNameValidator = _originalFullNameValidator;
		}
		else {
			_fullNameValidator = fullNameValidator;
		}
	}

	private static Log _log = LogFactoryUtil.getLog(
		FullNameValidatorFactory.class);

	private static FullNameValidator _fullNameValidator;
	private static FullNameValidator _originalFullNameValidator;

}
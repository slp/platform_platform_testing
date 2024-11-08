/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.platform.spectatio.configs.validators;

import com.google.common.base.VerifyException;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link ValidateSpectatioConfigForUnknownProperties} is an GSON Type Adaptor Factory for
 * validating the properties in Spectatio JSON Config.
 *
 * <p>GSON by default ignores any extra properties in the JSON configuration. This class ensures
 * that the JSON is checked for unknown properties and throw an exception if any unknown properties
 * are present.
 */
public class ValidateSpectatioConfigForUnknownProperties implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);

        if (!(delegate instanceof ReflectiveTypeAdapterFactory.Adapter)) {
            return delegate;
        }

        try {
            // Patch the reflection to be compatible with the changes in
            // ReflectiveTypeAdapterFactory
            // introduced by cl/483676181.
            // Another patch to the reflection to be compatible with changes in cl/571967573.
            Field f = findField(ReflectiveTypeAdapterFactory.Adapter.class, "fieldsData");
            f.setAccessible(true);
            Object fieldsData = f.get(gson.getDelegateAdapter(this, type));
            Field deserializedFieldsField = findField(fieldsData.getClass(), "deserializedFields");
            deserializedFieldsField.setAccessible(true);
            Map boundFieldsMap =
                    new LinkedHashMap((Map) deserializedFieldsField.get(fieldsData)) {
                        @Override
                        public Object get(Object key) {
                            Object value = super.get(key);
                            if (value == null) {
                                throw new VerifyException(
                                        String.format(
                                                "Unknown property %s in Spectatio JSON Config.",
                                                key));
                            }
                            return value;
                        }
                    };
            deserializedFieldsField.set(fieldsData, boundFieldsMap);
        } catch (Exception ex) {
            throw new VerifyException(ex);
        }

        return delegate;
    }

    private static Field findField(Class<?> startingClass, String fieldName)
            throws NoSuchFieldException {
        for (Class<?> c = startingClass; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // OK: continue with superclasses
            }
        }
        throw new NoSuchFieldException(fieldName + " starting from " + startingClass.getName());
    }
}

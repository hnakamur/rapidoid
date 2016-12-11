package org.rapidoid.render.retriever;

/*
 * #%L
 * rapidoid-render
 * %%
 * Copyright (C) 2014 - 2016 Nikolche Mihajlovski and contributors
 * %%
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
 * #L%
 */

import org.rapidoid.RapidoidThing;
import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.beany.Prop;

@Authors("Nikolche Mihajlovski")
@Since("5.3.0")
public class CachedPropRetriever extends RapidoidThing {

	private final Class<?> cachedModelType;
	private final Prop cachedProp;

	CachedPropRetriever(Class<?> cachedModelType, Prop cachedProp) {
		this.cachedModelType = cachedModelType;
		this.cachedProp = cachedProp;
	}

	boolean canRetrieve(Class<?> cls) {
		return cls.equals(cachedModelType);
	}

	Object retrieve(Object target) {
		return cachedProp.getFast(target);
	}

}

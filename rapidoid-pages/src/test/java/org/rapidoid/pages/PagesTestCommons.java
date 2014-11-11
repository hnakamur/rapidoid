package org.rapidoid.pages;

/*
 * #%L
 * rapidoid-pages
 * %%
 * Copyright (C) 2014 Nikolche Mihajlovski
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

import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

import org.rapidoid.html.TagContext;
import org.rapidoid.html.Var;
import org.rapidoid.http.HttpExchange;
import org.rapidoid.test.TestCommons;

public class PagesTestCommons extends TestCommons {

	@SuppressWarnings({ "unchecked" })
	protected static final Map<Integer, Object> NO_CHANGES = Collections.EMPTY_MAP;

	protected void print(TagContext ctx, PageComponent c) {
		HttpExchange x = mockExchange(ctx);
		String html = c.toHTML(x);
		notNull(html);
		System.out.println(html);
	}

	protected void has(TagContext ctx, PageComponent c, String... containingTexts) {
		HttpExchange x = mockExchange(ctx);
		String html = c.toHTML(x);
		notNull(html);

		for (String text : containingTexts) {
			isTrue(html.contains(text));
		}
	}

	protected void hasRegex(TagContext ctx, PageComponent c, String... containingRegexes) {
		HttpExchange x = mockExchange(ctx);
		String html = c.toHTML(x);
		notNull(html);

		for (String regex : containingRegexes) {
			isTrue(Pattern.compile(regex).matcher(html).find());
		}
	}

	protected void eq(Var<?> var, Object value) {
		eq(var.get(), value);
	}

	protected HttpExchange mockExchange(TagContext ctx) {
		HttpExchange x = mock(HttpExchange.class);
		returns(x.session(Pages.SESSION_CTX, null), ctx);
		return x;
	}

}

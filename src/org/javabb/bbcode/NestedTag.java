/*
 * Copyright 1998-2009 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

/*
 * Copyright 2004 JavaFree.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.javabb.bbcode;

/**
 * @author
 * @since 18/01/2005
 */
public interface NestedTag {
    /**
     * @return tag name
     */
    String getTagName();

    /**
     * @param buffer
     */
    void processContent(
        StringBuffer buffer,
        String tagName,
        String openSubstWithParam,
        String closeSubstWithParam,
        String openSubstWithoutParam,
        String closeSubstWithoutParam,
        String internalSubst,
        boolean processInternalTags,
        boolean acceptParam,
        boolean requiresQuotedParam);
}

/*
 * Copyright 2016 Crown Copyright
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

package stroom.util.io;

import java.io.IOException;
import java.io.InputStream;

public class IgnoreCloseInputStream extends WrappedInputStream {

    public IgnoreCloseInputStream(final InputStream inputStream) {
        super(inputStream);
    }

    public static IgnoreCloseInputStream wrap(final InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }

        return new IgnoreCloseInputStream(inputStream);
    }

    @Override
    public void close() throws IOException {
        // Ignore calls to close the stream.
    }
}

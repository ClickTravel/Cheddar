/*
 * Copyright 2014 Click Travel Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.clicktravel.infrastructure.persistence.aws.elasticache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.clicktravel.cheddar.infrastructure.persistence.cache.ItemCache;

import net.spy.memcached.MemcachedClient;

public class MemcachedItemCache implements ItemCache {

    private final MemcachedClient memcachedClient;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public MemcachedItemCache(final MemcachedClient memcachedClient) {
        this.memcachedClient = memcachedClient;
    }

    @Override
    public Object getItem(final String key, final long timeout) {
        Object item = null;
        final Future<Object> f = memcachedClient.asyncGet(key);
        try {
            item = f.get(timeout, TimeUnit.SECONDS);
        } catch (final TimeoutException e) {
            f.cancel(false);
            logger.trace("Unable to get cache item within given time", e);
            return null;
        } catch (final InterruptedException | ExecutionException e) {
            logger.debug("Unable to get cache item", e);
        }
        return item;
    }

    @Override
    public void putItem(final String key, final Object item, final long expire) {
        if (expire < Integer.MIN_VALUE || expire > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(expire + " cannot be cast to int without changing its value.");
        }
        memcachedClient.set(key, (int) expire, item);
    }
}

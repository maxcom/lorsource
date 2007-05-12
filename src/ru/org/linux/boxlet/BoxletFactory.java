package ru.org.linux.boxlet;

import java.io.IOException;
import java.util.Properties;

import ru.org.linux.util.ProfileHashtable;
import ru.org.linux.util.UtilException;

public abstract class BoxletFactory
{
        final Object config;

        public BoxletFactory(Object Config)
        {
                config=Config;
        }

        public abstract String getContent(String name, ProfileHashtable profile) throws IOException, BoxletException, UtilException;
        public abstract String getMenuContent(String name, ProfileHashtable profile, String addUrl, String removeUrl) throws IOException, BoxletException, UtilException;

        abstract String getVariantID(String name, ProfileHashtable profile, Properties request) throws BoxletException, UtilException;
        abstract long getVersionID(String name, ProfileHashtable profile, Properties request) throws BoxletException;
}

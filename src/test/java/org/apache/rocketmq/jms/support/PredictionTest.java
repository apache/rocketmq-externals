package org.apache.rocketmq.jms.support;

import java.util.Date;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class PredictionTest {

    @Test
    public void checkNotNull() throws Exception {
        Prediction.checkNotNull(new Date(), "Date could not be null");

        try {
            Prediction.checkNotNull(null, "Argument could not be null");
            assertThat("Haven't throw IllegalArgumentException", false);
        }
        catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Argument could not be null"));
        }
    }

    @Test
    public void checkNotBlank() throws Exception {
        Prediction.checkNotBlank("name", "Name could not be null");

        try {
            Prediction.checkNotBlank(null, "Name could not be null");
            assertThat("Haven't throw IllegalArgumentException", false);
        }
        catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Name could not be null"));
        }

        try {
            Prediction.checkNotBlank(" ", "Name could not be null");
            assertThat("Haven't throw IllegalArgumentException", false);
        }
        catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Name could not be null"));
        }
    }

}
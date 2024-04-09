package br.com.archbase.semver.implementation;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;


@RunWith(Enclosed.class)
public class MetadataVersionTest {

    public static class CoreFunctionalityTest {

        @Test
        public void mustCompareEachIdentifierSeparately() {
            MetadataVersion v1 = new MetadataVersion(
                    new String[]{"beta", "2", "abc"}
            );
            MetadataVersion v2 = new MetadataVersion(
                    new String[]{"beta", "1", "edf"}
            );
            assertTrue(0 < v1.compareTo(v2));
        }

        @Test
        public void shouldCompareIdentifiersCountIfCommonIdentifiersAreEqual() {
            MetadataVersion v1 = new MetadataVersion(
                    new String[]{"beta", "abc"}
            );
            MetadataVersion v2 = new MetadataVersion(
                    new String[]{"beta", "abc", "def"}
            );
            assertTrue(0 > v1.compareTo(v2));
        }

        @Test
        public void shouldComapareDigitsOnlyIdentifiersNumerically() {
            MetadataVersion v1 = new MetadataVersion(
                    new String[]{"alpha", "123"}
            );
            MetadataVersion v2 = new MetadataVersion(
                    new String[]{"alpha", "321"}
            );
            assertTrue(0 > v1.compareTo(v2));
        }

        @Test
        public void shouldCompareMixedIdentifiersLexicallyInAsciiSortOrder() {
            MetadataVersion v1 = new MetadataVersion(
                    new String[]{"beta", "abc"}
            );
            MetadataVersion v2 = new MetadataVersion(
                    new String[]{"beta", "111"}
            );
            assertTrue(0 < v1.compareTo(v2));
        }

        @Test
        public void shouldReturnNegativeWhenComparedToNullMetadataVersion() {
            MetadataVersion v1 = new MetadataVersion(new String[]{});
            MetadataVersion v2 = MetadataVersion.metadataNull;
            assertTrue(0 > v1.compareTo(v2));
        }

        @Test
        public void shouldOverrideEqualsMethod() {
            MetadataVersion v1 = new MetadataVersion(
                    new String[]{"alpha", "123"}
            );
            MetadataVersion v2 = new MetadataVersion(
                    new String[]{"alpha", "123"}
            );
            MetadataVersion v3 = new MetadataVersion(
                    new String[]{"alpha", "321"}
            );
            assertEquals(v1, v2);
            assertNotEquals(v1, v3);
        }

        @Test
        public void shouldProvideIncrementMethod() {
            MetadataVersion v1 = new MetadataVersion(
                    new String[]{"alpha", "1"}
            );
            MetadataVersion v2 = v1.increment();
            assertEquals("alpha.2", v2.toString());
        }

        @Test
        public void shouldAppendOneAsLastIdentifierIfLastOneIsAlphaNumericWhenIncrementing() {
            MetadataVersion v1 = new MetadataVersion(
                    new String[]{"alpha"}
            );
            MetadataVersion v2 = v1.increment();
            assertEquals("alpha.1", v2.toString());
        }

        @Test
        public void shouldBeImmutable() {
            MetadataVersion v1 = new MetadataVersion(
                    new String[]{"alpha", "1"}
            );
            MetadataVersion v2 = v1.increment();
            assertNotSame(v1, v2);
        }
    }

    public static class NullMetadataVersionTest {

        @Test
        public void shouldReturnEmptyStringOnToString() {
            MetadataVersion v = MetadataVersion.metadataNull;
            assertTrue(v.toString().isEmpty());
        }

        @Test
        public void shouldReturnZeroOnHashCode() {
            MetadataVersion v = MetadataVersion.metadataNull;
            assertEquals(0, v.hashCode());
        }

        @Test
        public void shouldBeEqualOnlyToItsType() {
            MetadataVersion v1 = MetadataVersion.metadataNull;
            MetadataVersion v2 = MetadataVersion.metadataNull;
            MetadataVersion v3 = new MetadataVersion(new String[]{});
            assertEquals(v1, v2);
            assertEquals(v2, v1);
            assertNotEquals(v1, v3);
        }

        @Test
        public void shouldReturnPositiveWhenComparedToNonNullMetadataVersion() {
            MetadataVersion v1 = MetadataVersion.metadataNull;
            MetadataVersion v2 = new MetadataVersion(new String[]{});
            assertTrue(0 < v1.compareTo(v2));
        }

        @Test
        public void shouldReturnZeroWhenComparedToNullMetadataVersion() {
            MetadataVersion v1 = MetadataVersion.metadataNull;
            MetadataVersion v2 = MetadataVersion.metadataNull;
            assertEquals(0, v1.compareTo(v2));
        }

        @Test
        public void shouldThrowNullPointerExceptionIfIncremented() {
            try {
                MetadataVersion.metadataNull.increment();
            } catch (NullPointerException e) {
                return;
            }
            fail("Deve lançar NullPointerException quando incrementado");
        }
    }

    public static class EqualsMethodTest {

        @Test
        @SuppressWarnings("java:S5863")
        public void shouldBeReflexive() {
            MetadataVersion v = new MetadataVersion(
                    new String[]{"alpha", "123"}
            );
            assertEquals(v, v);
        }

        @Test
        public void shouldBeSymmetric() {
            MetadataVersion v1 = new MetadataVersion(
                    new String[]{"alpha", "123"}
            );
            MetadataVersion v2 = new MetadataVersion(
                    new String[]{"alpha", "123"}
            );
            assertEquals(v1, v2);
            assertEquals(v2, v1);
        }

        @Test
        public void shouldBeTransitive() {
            MetadataVersion v1 = new MetadataVersion(
                    new String[]{"alpha", "123"}
            );
            MetadataVersion v2 = new MetadataVersion(
                    new String[]{"alpha", "123"}
            );
            MetadataVersion v3 = new MetadataVersion(
                    new String[]{"alpha", "123"}
            );
            assertEquals(v1, v2);
            assertEquals(v2, v3);
            assertEquals(v1, v3);
        }

        @Test
        public void shouldBeConsistent() {
            MetadataVersion v1 = new MetadataVersion(
                    new String[]{"alpha", "123"}
            );
            MetadataVersion v2 = new MetadataVersion(
                    new String[]{"alpha", "123"}
            );
            assertEquals(v1, v2);
            assertEquals(v1, v2);
            assertEquals(v1, v2);
        }

        @Test
        @SuppressWarnings("java:S5785")
        public void shouldReturnFalseIfOtherVersionIsOfDifferentType() {
            MetadataVersion v = new MetadataVersion(
                    new String[]{"alpha", "123"}
            );
            assertFalse(v.equals(new String("alpha.123")));
        }

        @Test
        public void shouldReturnFalseIfOtherVersionIsNull() {
            MetadataVersion v1 = new MetadataVersion(
                    new String[]{"alpha", "123"}
            );
            MetadataVersion v2 = null;
            assertNotEquals(v1, v2);
        }
    }

    public static class HashCodeMethodTest {

        @Test
        public void shouldReturnSameHashCodeIfVersionsAreEqual() {
            MetadataVersion v1 = new MetadataVersion(
                    new String[]{"alpha", "123"}
            );
            MetadataVersion v2 = new MetadataVersion(
                    new String[]{"alpha", "123"}
            );
            assertEquals(v1, v2);
            assertEquals(v1.hashCode(), v2.hashCode());
        }
    }

    public static class ToStringMethodTest {

        @Test
        public void shouldReturnStringRepresentation() {
            String value = "beta.abc.def";
            MetadataVersion v = new MetadataVersion(value.split("\\."));
            assertEquals(value, v.toString());
        }
    }
}

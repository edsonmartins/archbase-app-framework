package br.com.archbase.semver.implementation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class VersionParserTest {

    @Test
    public void shouldParseNormalVersion() {
        NormalVersion version = VersionParser.parseVersionCore("1.0.0");
        assertEquals(new NormalVersion(1, 0, 0), version);
    }

    @Test
    public void shouldRaiseErrorIfNumericIdentifierHasLeadingZeroes() {
        try {
            VersionParser.parseVersionCore("01.1.0");
        } catch (ParseException e) {
            return;
        }
        fail("O identificador numérico NÃO DEVE conter zeros à esquerda");
    }

    @Test
    public void shouldParsePreReleaseVersion() {
        MetadataVersion preRelease = VersionParser.parsePreRelease("beta-1.1");
        assertEquals(new MetadataVersion(new String[]{"beta-1", "1"}), preRelease);
    }

    @Test
    public void shouldNotAllowDigitsInPreReleaseVersion() {
        try {
            VersionParser.parsePreRelease("alpha.01");
        } catch (ParseException e) {
            return;
        }
        fail("Não deve permitir dígitos na versão de pré-lançamento");
    }

    @Test
    public void shouldRaiseErrorForEmptyPreReleaseIdentifier() {
        try {
            VersionParser.parsePreRelease("beta-1..1");
        } catch (ParseException e) {
            return;
        }
        fail("Os identificadores NÃO DEVEM estar vazios");
    }

    @Test
    public void shouldParseBuildMetadata() {
        MetadataVersion build = VersionParser.parseBuild("build.1");
        assertEquals(new MetadataVersion(new String[]{"build", "1"}), build);
    }

    @Test
    public void shouldAllowDigitsInBuildMetadata() {
        try {
            VersionParser.parseBuild("build.01");
        } catch (ParseException e) {
            fail("Deve permitir dígitos nos metadados de construção");
        }
    }

    @Test
    public void shouldRaiseErrorForEmptyBuildIdentifier() {
        try {
            VersionParser.parseBuild(".build.01");
        } catch (ParseException e) {
            return;
        }
        fail("Os identificadores NÃO DEVEM estar vazios");
    }

    @Test
    public void shouldParseValidSemVer() {
        VersionParser parser = new VersionParser("1.0.0-rc.2+build.05");
        Version version = parser.parse(null);
        assertEquals(
                new Version(
                        new NormalVersion(1, 0, 0),
                        new MetadataVersion(new String[]{"rc", "2"}),
                        new MetadataVersion(new String[]{"build", "05"})
                ),
                version
        );
    }

    @Test
    public void shouldRaiseErrorForIllegalInputString() {
        for (String illegal : new String[]{"", null}) {
            try {
                new VersionParser(illegal);
            } catch (IllegalArgumentException e) {
                continue;
            }
            fail("Deve gerar erro para string de entrada ilegal");
        }
    }
}

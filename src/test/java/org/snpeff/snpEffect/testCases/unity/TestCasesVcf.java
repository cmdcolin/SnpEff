package org.snpeff.snpEffect.testCases.unity;

import org.junit.jupiter.api.Test;
import org.snpeff.fileIterator.VcfFileIterator;
import org.snpeff.interval.Variant;
import org.snpeff.interval.Variant.VariantType;
import org.snpeff.interval.VariantBnd;
import org.snpeff.outputFormatter.VcfOutputFormatter;
import org.snpeff.snpEffect.EffectType;
import org.snpeff.util.Log;
import org.snpeff.util.Timer;
import org.snpeff.vcf.*;
import org.snpeff.vcf.VcfHeaderInfo.VcfInfoNumber;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VCF parsing test cases
 *
 * @author pcingola
 */
public class TestCasesVcf extends TestCasesBase {

    boolean createOutputFile = false;

    public TestCasesVcf() {
        super();
    }

    /**
     * Check that the size is correct (at least for SNPs)
     * <p>
     * Louis Letourneau discovered this horrendous bug. This is
     * my first attempt to fix it....
     */
    @Test
    public void test_00() {
        String fileName = path("1kg_head.vcf");
        VcfFileIterator vcf = new VcfFileIterator(fileName);

        for (VcfEntry ve : vcf) {
            assertEquals(1, ve.size());
        }
    }

    /**
     * Basic parsing
     */
    @Test
    public void test_01() {
        String fileName = path("vcf.vcf");
        VcfFileIterator vcf = new VcfFileIterator(fileName);
        vcf.setCreateChromos(true);
        for (VcfEntry vcfEntry : vcf) {
            for (Variant variant : vcfEntry.variants()) {
                String variantStr = "chr" + variant.toStringOld();
                if (verbose) Log.info(variant + "\t'" + variantStr + "'");
                assertEquals(variant.getId(), variantStr);
            }
        }
    }

    /**
     * Deletions
     */
    @Test
    public void test_04_del() {
        String fileName = path("vcf_04_del.vcf");
        VcfFileIterator vcf = new VcfFileIterator(fileName);
        vcf.setCreateChromos(true);
        for (VcfEntry vcfEntry : vcf) {
            for (Variant variant : vcfEntry.variants()) {
                if (!variant.isDel())
                    throw new RuntimeException("All VCF entries in this file should be deletions!\n\t" + variant);
            }
        }
    }

    /**
     * Problems parsing
     */
    @Test
    public void test_05_choking_on_dot_slash_dot() {
        String fileName = path("choking_on_dot_slash_dot.vcf");
        VcfFileIterator vcf = new VcfFileIterator(fileName);
        vcf.setCreateChromos(true);
        for (VcfEntry vcfEntry : vcf) {
            for (VcfGenotype gen : vcfEntry) {
                boolean var = gen.isVariant(); // This used to cause an exception
                if (verbose) Log.info("\t" + var + "\t" + gen);
            }
        }
        if (verbose) Log.info("");
    }

    /**
     * Problems creating variants
     * <p>
     * The problem is when creating a variant from this line:
     * Chr1    223919  .   CTCGACCACTGGAA  CTCACATCCATACAT,CATGACCACTGGAA
     * <p>
     * There are two changes:
     * CTCGACCACTGGAA
     * CTCACATCCATACAT
     * => GACCACTGGAA / ACATCCATACAT  (Mixed change?)
     * <p>
     * CTCGACCACTGGAA
     * CATGACCACTGGAA
     * ^^
     * => CG / TG  (MNP)
     */
    @Test
    public void test_06_mixed_change() {
        // WARNING: This test is expected to fail, because this functionality is unimplemented
        String file = path("array_out_of_bounds.vcf");

        VcfFileIterator vcf = new VcfFileIterator(file);
        vcf.setCreateChromos(true);

        for (VcfEntry vcfEntry : vcf) {
            if (verbose) Log.info(vcfEntry);

            // Compare variants to what we expect
            List<Variant> variants = vcfEntry.variants();

            assertEquals("chr1:223921_GACCACTGGAA/ACATCCATACAT", variants.get(0).toString());
            assertEquals("chr1:223919_TC/AT", variants.get(1).toString());
        }
    }

    /**
     * Extremely weird long lines in a VCF file (thousands of bases long)
     */
    @Test
    public void test_07_long_lines() {

        String file = path("long.vcf");

        Timer t = new Timer();
        t.start();

        VcfFileIterator vcf = new VcfFileIterator(file);
        vcf.setCreateChromos(true);

        // They are so long that they may produce 'Out of memory' errors
        for (VcfEntry vcfEntry : vcf) {
            if (verbose) Log.info(vcfEntry.getChromosomeName() + ":" + vcfEntry.getStart());
            for (VcfGenotype vg : vcfEntry)
                if (verbose) Log.info("\t" + vg);
        }

        // Too much time? we are doing something wrong...
        if (t.elapsed() > 1000) throw new RuntimeException("It should not take this long to process a few lines!!!");
    }

    /**
     * Test for "<DEL>" in ALT field
     */
    @Test
    public void test_08_alt_del() {

        String file = path("alt_del.vcf");

        VcfFileIterator vcf = new VcfFileIterator(file);
        vcf.setCreateChromos(true);

        // They are so long that they may produce 'Out of memory' errors
        for (VcfEntry vcfEntry : vcf) {
            if (verbose) Log.info(vcfEntry);

            boolean hasDel = false;
            for (Variant sc : vcfEntry.variants()) {
                hasDel |= sc.isDel();
                if (verbose) Log.info("\t" + sc + "\t" + sc.isDel());
            }

            assertEquals(true, hasDel);
        }
    }

    /**
     * Empty ALT: Not a variant
     */
    @Test
    public void test_09_empty_ALT() {
        String file = path("empty.vcf");

        VcfFileIterator vcf = new VcfFileIterator(file);
        for (VcfEntry vcfEntry : vcf) {
            if (verbose) Log.info(vcfEntry);
            assertEquals(false, vcfEntry.isVariant());
        }
    }

    /**
     * Empty Quality: Not a variant
     */
    @Test
    public void test_10_empty_QUAL() {
        String file = path("empty.vcf");

        VcfFileIterator vcf = new VcfFileIterator(file);
        for (VcfEntry vcfEntry : vcf) {
            if (verbose) Log.info(vcfEntry);
            assertEquals(0.0, vcfEntry.getQuality());
        }
    }

    /**
     * Empty fields should show '.' when printed
     */
    @Test
    public void test_11_empty() {
        String file = path("empty.vcf");

        VcfFileIterator vcf = new VcfFileIterator(file);
        for (VcfEntry vcfEntry : vcf) {
            if (verbose) Log.info(vcfEntry);
            assertEquals("1\t11169327\t.\tT\t.\t.\tPASS\tAC=0;AF=0.00;AN=176;DP=7756;MQ0=0;set=ReferenceInAll\tGT:DP\t0/0:115", vcfEntry.toString());
        }
    }

    @Test
    public void test_12_readHeader() {
        String file = path("test.chr1.1line.vcf");

        VcfFileIterator vcfFile = new VcfFileIterator(file);
        vcfFile.readHeader();

        int numLines = 0;
        for (VcfEntry vcfEntry : vcfFile) {
            if (verbose) Log.info(vcfEntry);
            numLines++;
        }

        assertEquals(1, numLines);
    }

    /**
     * Header should NOT have a trailing '\n'
     */
    @Test
    public void test_12_readHeader_NL() {
        String file = path("test.chr1.1line.vcf");

        VcfFileIterator vcfFile = new VcfFileIterator(file);
        String header = vcfFile.readHeader().toString();

        assertEquals(false, header.charAt(header.length() - 1) == '\n');
    }

    @Test
    public void test_13_chrOri() {
        String file = path("test.chr1.1line.vcf");

        VcfFileIterator vcfFile = new VcfFileIterator(file);
        vcfFile.readHeader();

        String chr = null;
        for (VcfEntry vcfEntry : vcfFile)
            chr = vcfEntry.getChromosomeNameOri();

        assertEquals("chr1", chr);
    }

    @Test
    public void test_14_VcfInfoKey() {
        new VcfOutputFormatter((List<VcfEntry>) null);
        String testIn[] = {"Hi ", "Hi how;", "Hi how;are|", "Hi how;are|you,", "Hi how;are|you,doing=", "Hi how;are|you,doing=today(.)", ".ann"};
        String testOut[] = {"Hi_", "Hi_how_", "Hi_how_are_", "Hi_how_are_you_", "Hi_how_are_you_doing_", "Hi_how_are_you_doing_today_._", "_.ann"};
        for (int i = 0; i < testIn.length; i++) {
            String safe = VcfEntry.vcfInfoKeySafe(testIn[i]);
            if (verbose) Log.info("'" + testIn[i] + "'\t'" + safe + "'\t'" + testOut[i] + "'");
            assertEquals(testOut[i], safe);
        }
    }

    @Test
    public void test_14_VcfInfoValue() {
        new VcfOutputFormatter((List<VcfEntry>) null);
        String testIn[] = {"Hi ", "Hi how;", "Hi how;are|", "Hi how;are|you,", "Hi how;are|you,doing=", "Hi how;are|you,doing=today(.)"};
        String testOut[] = {"Hi_", "Hi_how_", "Hi_how_are_", "Hi_how_are_you_", "Hi_how_are_you_doing_", "Hi_how_are_you_doing_today_._"};
        for (int i = 0; i < testIn.length; i++) {
            String safe = VcfEntry.vcfInfoValueSafe(testIn[i]);
            if (verbose) Log.info("'" + testIn[i] + "'\t'" + safe + "'\t'" + testOut[i] + "'");
            assertEquals(testOut[i], safe);
        }
    }

    @Test
    public void test_15_Eff_format_version_guess() {
        String vcfFileName = path("test.EFF_V2.vcf");
        EffFormatVersion formatVersion = formatVersion(vcfFileName);
        assertEquals(EffFormatVersion.FORMAT_EFF_2, formatVersion);

        vcfFileName = path("test.EFF_V3.vcf");
        formatVersion = formatVersion(vcfFileName);
        assertEquals(EffFormatVersion.FORMAT_EFF_3, formatVersion);
    }

    @Test
    public void test_16_indels() {
        String vcfFile = path("1kg.indels.vcf");

        VcfFileIterator vcf = new VcfFileIterator(vcfFile);
        for (VcfEntry ve : vcf) {
            if (verbose) Log.info(ve);
            StringBuilder variantResult = new StringBuilder();

            for (Variant v : ve.variants()) {
                if (variantResult.length() > 0) variantResult.append(",");

                String vs = v.toStringOld();
                vs = vs.substring(vs.indexOf('_') + 1);

                if (verbose) Log.info("\t" + v + "\t" + v.toStringOld() + "\t" + vs);

                variantResult.append(vs);
            }

            String variantExpected = ve.getInfo("SEQCHANGE");

            assertEquals(variantExpected, variantResult.toString());
        }
    }

    @Test
    public void test_18_vcf_tabix() {
        VcfFileIterator vcf = new VcfFileIterator(path("test_tabix.vcf.gz"));

        String chrpos = "";
        for (VcfEntry ve : vcf) {
            if (verbose) Log.info(ve);
            chrpos += ve.getChromosomeName() + ":" + ve.getStart() + " ";
        }

        // Make sure both lines appear
        assertEquals("1:249211906 2:41612", chrpos.trim());
    }

    @Test
    public void test_22_huge_headers() {
        String vcfFile = path("huge_header_slow.vcf.gz");

        Timer timer = new Timer();
        timer.start();

        VcfFileIterator vcf = new VcfFileIterator(vcfFile);
        for (VcfEntry ve : vcf) {
            if (verbose) Log.info(ve);
        }

        assertTrue(timer.elapsed() < 1000); // We should be able to iterate the whole file in less than a second
    }

    @Test
    public void test_23_VcfUnsorted() {
        String vcfFile = path("out_of_order.vcf");

        VcfFileIterator vcf = new VcfFileIterator(vcfFile);
        vcf.setErrorIfUnsorted(true);

        boolean errorFound = false;
        String expectedErrorMessage = "VCF file 'tests/unity/vcf/out_of_order.vcf' is not sorted, genomic position 20:2622038 is before 20:2621729";
        try {
            for (VcfEntry ve : vcf) {
                if (verbose) Log.info(ve);
            }
        } catch (Throwable e) {
            errorFound = e.getMessage().startsWith(expectedErrorMessage);
            if (verbose) e.printStackTrace();

            if (!errorFound) {
                Log.debug("Error messages differ:" //
                        + "\n\tExpected : '" + expectedErrorMessage + "'" //
                        + "\n\tActual   : '" + e.getMessage() + "'" //
                );
            }
        }

        assertTrue(errorFound);
    }

    /**
     * Parsing effect that created an exception (going from SO -> Classic)
     */
    @Test
    public void test_24_VcfEffect_parse_SO() {
        String vcfFileName = path("test_rasmus.vcf");

        VcfFileIterator vcf = new VcfFileIterator(vcfFileName);
        for (VcfEntry ve : vcf) {
            if (verbose) Log.info(ve);
            for (VcfEffect veff : ve.getVcfEffects())
                if (verbose) Log.info("\t\t" + veff);
        }
    }

    /**
     * Parsing Genomic VCFs
     * http://www.broadinstitute.org/gatk/guide/article?id=4017
     */
    @Test
    public void test_25_Genomic_VCF() {
        String vcfFileName = path("genomic_vcf.gvcf");

        VcfFileIterator vcf = new VcfFileIterator(vcfFileName);
        int start = -1;
        for (VcfEntry ve : vcf) {
            if (verbose) Log.info(ve);
            if (start < 0) start = ve.getStart();

            // Check
            if (start != ve.getStart())
                throw new RuntimeException("Start position should be " + start + " instead of " + ve.getStart() + "\n" + ve);

            boolean ok = false;
            for (Variant var : ve.variants()) {
                ok |= var.getVariantType().toString().equals(ve.getInfo("Type"));
                if (verbose)
                    Log.info(ve + "\n\t\tSize   : " + ve.size() + "\n\t\tVariant: " + ve.isVariant() + "\n\t\tType   : " + var.getVariantType() + "\n");
            }
            if (!ok) throw new RuntimeException("Variant type should be '" + ve.getInfo("Type") + "'\n" + ve);

            start = ve.getEnd() + 1;
        }
    }

    /**
     * Add a new INFO and the respective header
     */
    @Test
    public void test_26_vcfInfoHeaderAdd() {
        String vcfFileName = path("example_42.vcf");

        // Create a new INFO field
        String infoFieldName = "NEW_INFO";
        VcfHeaderInfo vhInfo = new VcfHeaderInfo(infoFieldName, VcfInfoType.Integer, VcfInfoNumber.UNLIMITED.toString(), "An arbitrary set of random numbers");
        String expectedHeader = "##INFO=<ID=" + infoFieldName + ",Number=.,Type=Integer,Description=\"An arbitrary set of random numbers\">";

        // Open VCF file
        VcfFileIterator vcf = new VcfFileIterator(vcfFileName);
        for (VcfEntry ve : vcf) {
            if (vcf.isHeaderSection()) {
                // Add INFO field to header
                vcf.getVcfHeader().addInfo(vhInfo);
                if (verbose) Log.info(vcf.getVcfHeader());
                assertTrue(vcf.getVcfHeader().toString().contains(expectedHeader));
            }

            // Add INFO field values
            String value = "" + ((int) (1000 * Math.random()));
            ve.addInfo(infoFieldName, value);
            if (verbose) Log.info(ve);

            // Check that 'info=value' is there
            assertTrue(ve.toString().contains(infoFieldName + "=" + value));
        }
    }

    /**
     * Add and replace an INFO header
     */
    @Test
    public void test_27_vcfInfoHeaderReplace() {

        String infoFieldName = "NEW_INFO";
        String vcfFileName = path("example_42.vcf");

        // Add this header
        VcfHeaderInfo vhInfo = new VcfHeaderInfo(infoFieldName, VcfInfoType.Integer, VcfInfoNumber.UNLIMITED.toString(), "An arbitrary set of integer random numbers");
        String expectedHeader = "##INFO=<ID=" + infoFieldName + ",Number=.,Type=Integer,Description=\"An arbitrary set of integer random numbers\">";

        // Replace using this header
        VcfHeaderInfo vhInfo2 = new VcfHeaderInfo(infoFieldName, VcfInfoType.Float, "1", "One float random number");
        String expectedHeader2 = "##INFO=<ID=" + infoFieldName + ",Number=1,Type=Float,Description=\"One float random number\">";

        // Open VCF file
        VcfFileIterator vcf = new VcfFileIterator(vcfFileName);
        for (VcfEntry ve : vcf) {
            if (vcf.isHeaderSection()) {
                // Add INFO field to header
                vcf.getVcfHeader().addInfo(vhInfo);
                if (verbose) Log.info(vcf.getVcfHeader());
                assertTrue(vcf.getVcfHeader().toString().contains(expectedHeader));

                // Add second INFO field to header (should replace first one)
                vcf.getVcfHeader().addInfo(vhInfo2);
                if (verbose) Log.info(vcf.getVcfHeader());
                assertTrue(vcf.getVcfHeader().toString().contains(expectedHeader2)); // New header
                assertTrue(!vcf.getVcfHeader().toString().contains(expectedHeader)); // Old header should be gone
            }

            // Add INFO field values
            String value = "" + ((int) (1000 * Math.random()));
            ve.addInfo(infoFieldName, value);
            if (verbose) Log.info(ve);

            // Check that 'info=value' is there
            assertTrue(ve.toString().contains(infoFieldName + "=" + value));
        }
    }

    /**
     * Add and replace an INFO header
     */
    @Test
    public void test_28_vcfInfoReplace() {

        String vcfFileName = path("example_42.vcf");

        // Replace all 'DP' fields using this value
        String infoKey = "DP";
        String infoValue = "42";

        // Open VCF file
        VcfFileIterator vcf = new VcfFileIterator(vcfFileName);
        for (VcfEntry ve : vcf) {
            String infoValuePrev = ve.getInfo(infoKey);

            // Check that 'key=value' is in INFO
            String keyValPrev = infoKey + "=" + infoValuePrev;
            assertTrue(ve.getInfoStr().contains(keyValPrev), "Old key=value is not present");

            // Replace value
            ve.addInfo(infoKey, infoValue);
            if (verbose) Log.info(ve);

            // Check that new 'key=value' is there
            String keyVal = infoKey + "=" + infoValue;
            assertTrue(ve.toString().contains(keyVal), "New key=value is present");

            // Check that previous 'key=value' is no longer there
            assertTrue(!ve.getInfoStr().contains(keyValPrev), "Old key=value is still in INOF field");
        }
    }

    /**
     * Test old effect separator '+' instead of '&'
     */
    @Test
    public void test_29() {
        String fileName = path("test_vcf_ann_plus_sign.vcf");
        VcfFileIterator vcf = new VcfFileIterator(fileName);

        for (VcfEntry ve : vcf) {
            if (verbose) Log.info(ve);

            for (VcfEffect veff : ve.getVcfEffects()) {
                if (verbose) Log.info("\t" + veff);

                // Check
                assertEquals(EffectType.UTR_5_DELETED.toString(), veff.getEffectTypes().get(0).toString());
                assertEquals(EffectType.EXON_DELETED.toString(), veff.getEffectTypes().get(1).toString());
            }
        }
    }

    /**
     * Non-variant gVCF entries (i.e. ALT=<NON_REF>)
     */
    @Test
    public void test_30_gVCF_NON_REF() {

        String vcfFileName = path("test_gVCF_NON_REF.vcf");

        VcfFileIterator vcf = new VcfFileIterator(vcfFileName);
        for (VcfEntry ve : vcf) {
            if (verbose) Log.info(ve);

            // Check variants
            // The last variant is "<NON_REF>" which is interpreted as non-variant (it gives no information)
            int countNonVariants = 0;
            for (Variant var : ve.variants()) {
                if (verbose) Log.info("\t" + var);
                if (!var.isVariant()) countNonVariants++;
            }
            assertEquals(1, countNonVariants);

            // Check that we can parse genotypes
            for (VcfGenotype vgt : ve.getVcfGenotypes()) {
                if (verbose) Log.info("\t\tVCF_GT: " + vgt);
            }

            // Check GT score
            for (byte gt : ve.getGenotypesScores()) {
                if (verbose) Log.info("\t\tGT    : " + gt);
                assertEquals(1, gt);
            }
        }
    }

    @Test
    public void test_31_MISSING_REF() {
        String vcfFile = path("test_missing_ref.vcf");
        VcfFileIterator vcf = new VcfFileIterator(vcfFile);

        int countVariants = 0;
        for (VcfEntry ve : vcf) {
            if (verbose) Log.info(ve);
            for (Variant v : ve.variants()) {
                if (verbose) Log.info("\t" + v + "\tis Variant: " + v.isVariant());
                if (v.isVariant()) countVariants++;
            }
        }

        assertEquals(1, countVariants);
    }

    @Test
    public void test_32_VcfInfoKeyNames() {
        String keysPass[] = {"ANN", "ann9", "a9nn", "ann_", "a_nn", "_ann", "ann.gene"};
        String keysFail[] = {"ann+", "9ann", ".gene"};

        for (String key : keysPass) {
            assertTrue(VcfEntry.isValidInfoKey(key), "String '" + key + "' should be a valid INFO key");
        }

        for (String key : keysFail)
            assertFalse(VcfEntry.isValidInfoKey(key), "String '" + key + "' should be an invalid INFO key");
    }

    @Test
    public void test_33_translocations() {
        String vcfFile = path("vcf_translocation.vcf");

        VcfFileIterator vcf = new VcfFileIterator(vcfFile);
        for (VcfEntry ve : vcf) {
            if (verbose) Log.info(ve);

            boolean ok = false;
            for (Variant var : ve.variants()) {
                if (verbose) Log.info("\t" + var.getVariantType() + "\t" + var);
                assertEquals(VariantType.BND, var.getVariantType(), "Variant type is not 'BND'");
                ok = true;
            }

            assertTrue(ok, "No variants found!");
        }
    }

    @Test
    public void test_34_vcfInfoEncoding() {
        String str = "hi;hello;bye;\nadios=chau\tbye\nhi=hello\thola";
        String enc = VcfEntry.vcfInfoEncode(str);
        String dec = VcfEntry.vcfInfoDecode(enc);
        assertEquals(str, dec, "Encoding-Decoding cycle failed");
    }

    @Test
    public void test_35_translocations_parsing() {
        String vcfFile = path("vcf_translocation_parsing.vcf");

        VcfFileIterator vcf = new VcfFileIterator(vcfFile);
        for (VcfEntry ve : vcf) {
            if (verbose) Log.info(ve);
            boolean ok = false;
            for (Variant var : ve.variants()) {
                if (verbose) Log.info("\t" + var.getVariantType() + "\t" + var);
                assertEquals(VariantType.BND, var.getVariantType(), "Variant type is not 'BND'");

                VariantBnd vbnd = (VariantBnd) var;
                assertEquals(ve.getInfoFlag("LEFT"), vbnd.isLeft(), "Variant BND 'left' does not match");
                assertEquals(ve.getInfoFlag("BEFORE"), vbnd.isBefore(), "Variant BND 'before' does not match");
                ok = true;
            }

            assertTrue(ok, "No variants found!");
        }
    }

    @Test
    public void test_36_cleanupUnderscores() {
        debug = true;
        assertEquals("", VcfEntry.cleanUnderscores(""));
        assertEquals("", VcfEntry.cleanUnderscores("_"));
        assertEquals("", VcfEntry.cleanUnderscores("__"));
        assertEquals("a", VcfEntry.cleanUnderscores("_a"));
        assertEquals("a", VcfEntry.cleanUnderscores("a_"));
        assertEquals("a", VcfEntry.cleanUnderscores("_a_"));
        assertEquals("a_z", VcfEntry.cleanUnderscores("a__z"));
        assertEquals("a_b_c_d", VcfEntry.cleanUnderscores("a_b_c_d_"));
        assertEquals("a1_b2_c3_d4", VcfEntry.cleanUnderscores("_a1__b2__c3__d4__"));
        assertEquals("a1_b2c3_d4", VcfEntry.cleanUnderscores("_____a1_b2c3_______d4________"));
    }

    @Test
    public void test_37_INV() {
        String vcfFile = path("test_inv.vcf");

        // Parameters as in the VCF file
        var ref = "GTGAGATGGGAGTTCAGCAGGGCCCGCGGCCCCTCGCCCTCCGCGAGCTCCCAGTCCCGCGTCCTCACCTCCAACATCTC";
        var start = 27360853;
        var end = 27360932;
        var svlen = 79;

        // Read the first entry
        VcfFileIterator vcf = new VcfFileIterator(vcfFile);
        VcfEntry ve = vcf.next();

        // Check VCF entry
        assertEquals(start - 1, ve.getStart());
        assertEquals(end - 1, ve.getEnd());
        assertEquals(ref, ve.getRef());
        assertEquals(svlen, Math.abs(ve.getInfoInt("SVLEN")));
        assertEquals("<INV>", ve.getAltsStr());
        // Check variant
        var vars = ve.variants();
        var inv = (Variant) vars.get(0);
        assertEquals(1, vars.size());
        assertEquals(VariantType.INV, inv.getVariantType());
        assertEquals(ref.substring(1), inv.getReference());
        var altVariant = new StringBuilder(ref.substring(1)).reverse().toString();
        assertEquals(altVariant, inv.getAlt());
        assertEquals(start + 1 - 1, inv.getStart()); // In VCF the first base on an <INV> is not included, so we add 1. It's zero-based, so we subtract 1
        assertEquals(end - 1, inv.getEnd());
    }

    @Test
    public void test_37_INV_2() {
        String vcfFile = path("test_inv_2.vcf");

        // Parameters as in the VCF file
        var ref = "CCTTTAGGGCCGGGACAGTGTCGTATATACTGGCTGCTCCCAGTGTGTGGGGCTGTGGGACT";
        var start = 37973492;
        var end = start + (ref.length() - 1);
        
        // Read the first entry
        VcfFileIterator vcf = new VcfFileIterator(vcfFile);
        VcfEntry ve = vcf.next();

        // Check VCF entry
        assertEquals(start - 1, ve.getStart());
        assertEquals(end - 1, ve.getEnd());
        assertEquals(ref, ve.getRef());
        assertEquals("<INV>", ve.getAltsStr());
        // Check variant
        var vars = ve.variants();
        var inv = (Variant) vars.get(0);
        var refExpected = ref.substring(1); // In VCF the first base on an <INV> is not included
        var altExpected = new StringBuilder(refExpected).reverse().toString();
        assertEquals(1, vars.size());
        assertEquals(VariantType.INV, inv.getVariantType());
        assertEquals(refExpected, inv.getReference());
        assertEquals(altExpected, inv.getAlt());
        assertEquals(start + 1 - 1, inv.getStart()); // In VCF the first base on an <INV> is not included, so we add 1. It's zero-based, so we subtract 1
        assertEquals(end - 1, inv.getEnd());        
    }

    @Test
    public void test_37_INV_3() {
        String vcfContent = "chrA\t14\t.\tT\t<INV>\t.\t.\tIMPRECISE;SVLEN=100";
        VcfFileIterator vi = VcfFileIterator.fromString(vcfContent);
        VcfEntry ve = vi.next();

        // Check VCF entry
        assertEquals("A", ve.getChromosomeName());
        assertEquals(14 - 1, ve.getStart());
        assertEquals(14 + (100 - 1) - 1, ve.getEnd());
        assertEquals(100, Math.abs(ve.getInfoInt("SVLEN")));
        Variant var = ve.variants().get(0);
        assertEquals(VariantType.INV, var.getVariantType());
        assertEquals(14, var.getStart());
        assertEquals(14 + (100 - 1) - 1, var.getEnd());
    }

    @Test
    public void test_38_DEL_01() {
        // In this case we have both 'END' and 'SVLEN' fields
        String vcfContent = "2\t321682\t.\tT\t<DEL>\t6\tPASS\tIMPRECISE;SVTYPE=DEL;END=321887;SVLEN=-206";
        VcfFileIterator vi = VcfFileIterator.fromString(vcfContent);
        VcfEntry ve = vi.next();

        // Check VCF entry
        assertEquals("2", ve.getChromosomeName());
        assertEquals(321682 - 1, ve.getStart());
        assertEquals(321887 - 1, ve.getEnd());
        assertEquals(321887 - 321682 + 1, Math.abs(ve.getInfoInt("SVLEN"))); // SVLEN is the number of bases in the REF field, including the first and last base
        // Check variant
        Variant var = ve.variants().get(0);
        assertEquals(VariantType.DEL, var.getVariantType());
        assertEquals(321682, var.getStart()); // The first base in VCF's REF is not included in the <DEL>
        assertEquals(321887 - 1, var.getEnd());
        assertTrue(var.getReference().startsWith("NNNNNNNNNNN")); // Ref was padded with Ns
        assertEquals("", var.getAlt());
    }

    @Test
    public void test_39_DEL_02() {
        // In this case we have 'SVLEN', but not 'END'
        String vcfContent = "chrA\t2\t.\tT\t<DEL>\t.\t.\tSVLEN=2";
        VcfFileIterator vi = VcfFileIterator.fromString(vcfContent);
        VcfEntry ve = vi.next();

        // Check VCF entry
        assertEquals("A", ve.getChromosomeName());
        assertEquals(2 - 1, ve.getStart());
        assertEquals(3 - 1, ve.getEnd());  // END = start + (SVLEN - 1) = 2 + (2 - 1) = 3. To zero-base index => 3 - 1
        assertEquals(2, Math.abs(ve.getInfoInt("SVLEN")));
        // Check variant
        Variant var = ve.variants().get(0);
        assertEquals(VariantType.DEL, var.getVariantType());
        assertEquals(2, var.getStart()); // The first base in VCF's REF is not included in the <DEL>
        assertEquals(2, var.getEnd());
        assertTrue(var.getReference().startsWith("N")); // Ref was padded with Ns (first base in REF is not included in the variant)
        assertEquals("", var.getAlt());
    }

    @Test
    public void test_39_DEL_03() {
        // In this case we have 'END', but not 'SVLEN'
        String vcfContent = "chrA\t2\t.\tT\t<DEL>\t.\t.\tEND=3";
        VcfFileIterator vi = VcfFileIterator.fromString(vcfContent);
        VcfEntry ve = vi.next();

        // Check VCF entry
        assertEquals("A", ve.getChromosomeName());
        assertEquals(2 - 1, ve.getStart());
        assertEquals(3 - 1, ve.getEnd());  // END = start + (SVLEN - 1) = 2 + (2 - 1) = 3. To zero-base index => 3 - 1
        // Check variant
        Variant var = ve.variants().get(0);
        assertEquals(VariantType.DEL, var.getVariantType());
        assertEquals(2, var.getStart()); // The first base in VCF's REF is not included in the <DEL>
        assertEquals(2, var.getEnd());
        assertTrue(var.getReference().startsWith("N")); // Ref was padded with Ns (first base in REF is not included in the variant)
        assertEquals("", var.getAlt());
    }

    @Test
    public void test_39_DEL_04() {
        // Long SVLEN (i.e. more than MAX_PADN bases). This tests for maximum 'padNs' when padding the REF field in the variant
        String vcfContent = "2\t321682\t.\tT\t<DEL>\t6\tPASS\tIMPRECISE;SVTYPE=DEL;END=323687;SVLEN=-2006;CIPOS=-56,20;CIEND=-10,62";
        VcfFileIterator vi = VcfFileIterator.fromString(vcfContent);
        VcfEntry ve = vi.next();

        // Check VCF entry
        assertEquals("2", ve.getChromosomeName());
        assertEquals(321681, ve.getStart());
        assertEquals(323687 - 1, ve.getEnd());
        assertEquals(2006, Math.abs(ve.getInfoInt("SVLEN")));
        // Check variant
        Variant var = ve.variants().get(0);
        assertEquals(VariantType.DEL, var.getVariantType());
        assertEquals(321682, var.getStart()); // The first base in VCF's REF is not included in the <DEL>
        assertEquals(323687 - 1, var.getEnd());
        assertTrue(var.getReference().startsWith("N")); // Ref was padded with Ns (first base in REF is not included in the variant)
        assertTrue(var.getReference().length() >= VcfEntry.MAX_PADN); // Ref was padded with Ns (first base in REF is not included in the variant)
        assertEquals("", var.getAlt());
    }

    @Test
    public void test_40_DUP() {
        String vcfContent = "chrA\t5\t.\tG\t<DUP>\t.\t.\tSVLEN=3;CIPOS=0,5;EVENT=homology_dup";
        VcfFileIterator vi = VcfFileIterator.fromString(vcfContent);
        VcfEntry ve = vi.next();

        // Check VCF entry
        assertEquals("A", ve.getChromosomeName());
        assertEquals(5 - 1, ve.getStart());
        assertEquals(7 - 1, ve.getEnd());
        assertEquals(3, Math.abs(ve.getInfoInt("SVLEN")));
        Variant var = ve.variants().get(0);
        assertEquals(VariantType.DUP, var.getVariantType());
        assertEquals(5 - 1 + 1, var.getStart()); // The first base in VCF's REF is not included in the <DEL>
        assertEquals(7 - 1, var.getEnd());
        assertEquals("G", var.getReference()); // Ref was padded with Ns (first base in REF is not included in the variant)
        assertEquals("NNNN", var.getAlt());
    }

    @Test
    public void test_41_INS() {
        String vcfContent = "chrA\t14\t.\tT\t<INS>\t.\t.\tIMPRECISE;SVLEN=100;CILEN=-50,50;CIPOS=-10,10";
        VcfFileIterator vi = VcfFileIterator.fromString(vcfContent);
        VcfEntry ve = vi.next();

        // Check VCF entry
        assertEquals("A", ve.getChromosomeName());
        assertEquals(14 - 1, ve.getStart());
        assertEquals(14 - 1, ve.getEnd());
        assertEquals(100, Math.abs(ve.getInfoInt("SVLEN")));
        // Check variant
        Variant var = ve.variants().get(0);
        assertEquals(VariantType.INS, var.getVariantType());
        assertEquals(14 - 1, var.getStart()); // The first base in VCF's REF is not included in the <DEL>
        assertEquals(14 - 1, var.getEnd());
        assertEquals("", var.getReference());
        assertEquals("<INS>", var.getAlt());
    }

    @Test
    public void test_43_CNV_01() {
        //  From the VCF Spec:
        //      For example, a region on chr1 from position 101 to 130 (both inclusive) with allele-specific copy numbers of 1 and 2 can be represented as follows:
        //          chr1 100 . T <CNV>,<CNV> . . SVLEN=30,30;CN=1,2 GT:CN 1/2:3
        String vcfContent = "chr1\t100\t.\tT\t<CNV>\t.\t.\tSVLEN=30";
        VcfFileIterator vi = VcfFileIterator.fromString(vcfContent);
        VcfEntry ve = vi.next();

        // Check VCF entry
        assertEquals("1", ve.getChromosomeName());
        assertEquals(100 - 1, ve.getStart());
        assertEquals(100 + (30 -1) - 1, ve.getEnd());
        assertEquals(30, Math.abs(ve.getInfoInt("SVLEN"))); // SVLEN is the number of bases in the REF field, including the first and last base
        // Check variant
        Variant var = ve.variants().get(0);
        assertEquals(VariantType.CNV, var.getVariantType());
        assertEquals(100, var.getStart()); // The first base in VCF's REF is not included in the <DEL>
        assertEquals(100 + (30 -1) - 1, var.getEnd());
        assertEquals(var.getReference(), "");
        assertEquals(var.getAlt(), "");
    }

    @Test
    public void test_44_deletion() {
        String vcfContent = "chr11\t66326794\t.\tAGAGC\tA\t.\t.\t.";
        VcfFileIterator vi = VcfFileIterator.fromString(vcfContent);
        VcfEntry ve = vi.next();
        
        // Check coordinates
        assertEquals("11", ve.getChromosomeName());
        assertEquals(66326794 - 1, ve.getStart());
        assertEquals(66326794 + 4 - 1, ve.getEnd());
        assertEquals(5, ve.getRef().length());
        assertEquals(1, ve.getAltsStr().length());
        // Check variant
        Variant var = ve.variants().get(0);
        assertEquals(VariantType.DEL, var.getVariantType());
        assertEquals(66326794, var.getStart());
        assertEquals(66326794 + 4 - 1, var.getEnd());
        assertEquals("GAGC", var.getReference());
        assertEquals("", var.getAlt());
    }



}

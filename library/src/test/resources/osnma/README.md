# OSNMA test data

These files come from the OSNMA test vectors published by the European GNSS Service Centre
(GSC), https://www.gsc-europa.eu/support-to-developers/osnma-test-vectors, which are intended for
receiver developers to validate their OSNMA implementations:

- `16_AUG_2023_GST_05_00_01_8min.csv.gz`: scenario "configuration 1" (16 August 2023, 05:00:01
  GST), trimmed to its first 8 minutes. One line per satellite with the concatenated 240-bit
  E1-B I/NAV pages, starting at WN 1251, TOW 277201 and 2 s apart.
- `OSNMA_MerkleTree_test_vectors_1.xml` and `OSNMA_PublicKey_test_vectors_PKID_1.xml`: the
  Merkle tree and public key of that scenario. They are test material only and unrelated to the
  operational Galileo keys.

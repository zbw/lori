package de.zbw.api.lori.server.type

import de.zbw.api.lori.server.connector.DAConnectorTest.Companion.TEST_COLLECTION
import de.zbw.api.lori.server.connector.DAConnectorTest.Companion.TEST_COMMUNITY
import de.zbw.api.lori.server.exception.InvalidIPAddressException
import de.zbw.api.lori.server.route.ErrorRoutesKtTest
import de.zbw.api.lori.server.route.QueryParameterParser
import de.zbw.business.lori.server.RightIdFilter
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.BasisAccessState
import de.zbw.business.lori.server.type.BasisStorage
import de.zbw.business.lori.server.type.Bookmark
import de.zbw.business.lori.server.type.ConflictType
import de.zbw.business.lori.server.type.Group
import de.zbw.business.lori.server.type.GroupEntry
import de.zbw.business.lori.server.type.Item
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.RightError
import de.zbw.business.lori.server.type.RightIdTemplateName
import de.zbw.business.lori.server.type.SearchQueryResult
import de.zbw.business.lori.server.type.TemplateApplicationResult
import de.zbw.lori.model.AccessStateWithCountRest
import de.zbw.lori.model.IsPartOfSeriesCountRest
import de.zbw.lori.model.ItemInformation
import de.zbw.lori.model.ItemRest
import de.zbw.lori.model.LicenceUrlCountRest
import de.zbw.lori.model.MetadataRest
import de.zbw.lori.model.PaketSigelWithCountRest
import de.zbw.lori.model.PublicationTypeWithCountRest
import de.zbw.lori.model.RightRest
import de.zbw.lori.model.TemplateApplicationRest
import de.zbw.lori.model.TemplateNameWithCountRest
import de.zbw.lori.model.ZdbIdWithCountRest
import de.zbw.persistence.lori.server.GroupDBTest.Companion.NOW
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.Assert
import org.testng.Assert.assertNull
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

class RestConverterTest {
    @Test
    fun testItemConversion() {
        // given
        val expected =
            Item(
                metadata = TEST_METADATA,
                rights = listOf(TEST_RIGHT),
            )

        val restObject =
            ItemRest(
                metadata =
                    MetadataRest(
                        author = TEST_METADATA.author,
                        band = TEST_METADATA.band,
                        collectionName = TEST_METADATA.collectionName,
                        collectionHandle = TEST_METADATA.collectionHandle,
                        communityHandle = TEST_METADATA.communityHandle,
                        communityName = TEST_METADATA.communityName,
                        createdBy = TEST_METADATA.createdBy,
                        createdOn = TEST_METADATA.createdOn,
                        deleted = TEST_METADATA.deleted,
                        doi = TEST_METADATA.doi,
                        handle = TEST_METADATA.handle,
                        isbn = TEST_METADATA.isbn,
                        issn = TEST_METADATA.issn,
                        isPartOfSeries = TEST_METADATA.isPartOfSeries,
                        lastUpdatedBy = TEST_METADATA.lastUpdatedBy,
                        lastUpdatedOn = TEST_METADATA.lastUpdatedOn,
                        licenceUrl = TEST_METADATA.licenceUrl,
                        paketSigel = TEST_METADATA.paketSigel,
                        ppn = TEST_METADATA.ppn,
                        publicationType = TEST_METADATA.publicationType.toRest(),
                        publicationYear = TEST_METADATA.publicationYear,
                        subCommunityHandle = TEST_METADATA.subCommunityHandle,
                        subCommunityName = TEST_METADATA.subCommunityName,
                        storageDate = TEST_METADATA.storageDate,
                        title = TEST_METADATA.title,
                        titleJournal = TEST_METADATA.titleJournal,
                        titleSeries = TEST_METADATA.titleSeries,
                        zdbIds = TEST_METADATA.zdbIds,
                    ),
                rights =
                    listOf(
                        RightRest(
                            rightId = TEST_RIGHT.rightId,
                            accessState = TEST_RIGHT.accessState?.toRest(),
                            basisAccessState = TEST_RIGHT.basisAccessState?.toRest(),
                            basisStorage = TEST_RIGHT.basisStorage?.toRest(),
                            createdBy = TEST_RIGHT.createdBy,
                            createdOn = TEST_RIGHT.createdOn,
                            endDate = TEST_RIGHT.endDate,
                            hasLegalRisk = TEST_RIGHT.hasLegalRisk,
                            hasExceptionId = TEST_RIGHT.hasExceptionId,
                            groupIds = TEST_RIGHT.groupIds,
                            groups = TEST_RIGHT.groups?.map { it.toRest() },
                            isTemplate = TEST_RIGHT.isTemplate,
                            lastAppliedOn = TEST_RIGHT.lastAppliedOn,
                            lastUpdatedBy = TEST_RIGHT.lastUpdatedBy,
                            lastUpdatedOn = TEST_RIGHT.lastUpdatedOn,
                            licenceContract = TEST_RIGHT.licenceContract,
                            notesGeneral = TEST_RIGHT.notesGeneral,
                            notesFormalRules = TEST_RIGHT.notesFormalRules,
                            notesProcessDocumentation = TEST_RIGHT.notesProcessDocumentation,
                            notesManagementRelated = TEST_RIGHT.notesManagementRelated,
                            restrictedOpenContentLicence = TEST_RIGHT.restrictedOpenContentLicence,
                            startDate = TEST_RIGHT.startDate,
                            templateDescription = TEST_RIGHT.templateDescription,
                            templateName = TEST_RIGHT.templateName,
                            zbwUserAgreement = TEST_RIGHT.zbwUserAgreement,
                        ),
                    ),
            )

        // when + then
        assertThat(restObject.toBusiness(), `is`(expected))
        assertThat(restObject.toBusiness().toRest(), `is`(restObject))
    }

    @Test
    fun testDAItemConverter() {
        // given
        val expected =
            ItemMetadata(
                author = "Colbjørnsen, Terje",
                band = null,
                collectionHandle = TEST_COLLECTION.handle,
                collectionName = TEST_COLLECTION.name,
                communityName = TEST_COMMUNITY.name,
                communityHandle = TEST_COMMUNITY.handle,
                createdBy = null,
                createdOn = null,
                deleted = false,
                doi = listOf("10.7298/c5ps-be97"),
                handle = "11159/848",
                isbn = listOf("9781847200235", "9781845420680"),
                issn = null,
                isPartOfSeries = listOf("seriespart"),
                lastUpdatedBy = null,
                lastUpdatedOn = null,
                licenceUrl = "https://creativecommons.org/licenses/by-sa/4.0/legalcode.de",
                licenceUrlFilter = "by-sa/4.0/legalcode.de",
                paketSigel = null,
                ppn = null,
                publicationType = PublicationType.ARTICLE,
                publicationYear = 2022,
                storageDate =
                    OffsetDateTime.of(
                        2022,
                        1,
                        19,
                        7,
                        57,
                        26,
                        0,
                        ZoneOffset.UTC,
                    ),
                subCommunityHandle = TEST_COMMUNITY.subcommunities?.get(0)!!.handle,
                subCommunityName = TEST_COMMUNITY.subcommunities?.get(0)!!.name,
                title = "some_title",
                titleJournal = "some_journal",
                titleSeries = "some_series",
                zdbIds = listOf("zdbId1", "zdbId2"),
            )

        // when
        val receivedItem =
            TEST_DA_ITEM.toBusiness(
                TEST_COMMUNITY,
                TEST_COLLECTION,
            )
        // then
        assertThat(receivedItem, `is`(expected))

        // when + then
        val receivedItem2 = TEST_DA_ITEM.copy(handle = null)
        assertThat(receivedItem2, `is`(receivedItem2))
    }

    @Test
    fun testParseToDate() {
        // when + then
        assertThat(
            RestConverter.parseToDate("2022"),
            `is`(LocalDate.of(2022, 1, 1)),
        )
        assertThat(
            RestConverter.parseToDate("2022-09"),
            `is`(LocalDate.of(2022, 9, 1)),
        )
        assertThat(
            RestConverter.parseToDate("2022-09-02"),
            `is`(LocalDate.of(2022, 9, 2)),
        )
        assertThat(
            RestConverter.parseToDate("2022/09"),
            `is`(LocalDate.of(2022, 9, 1)),
        )
        assertNull(
            RestConverter.parseToDate("foo"),
        )
    }

    @Test
    fun testGroupConverter() {
        val givenGroup =
            Group(
                groupId = 1,
                description = "description",
                entries =
                    listOf(
                        GroupEntry(
                            organisationName = "some orga",
                            ipAddresses = "123.456.1.127",
                        ),
                    ),
                title = "some title",
                createdOn = NOW.minusMonths(1L),
                lastUpdatedOn = NOW,
                createdBy = "user1",
                lastUpdatedBy = "user2",
                version = 0,
                oldVersions = emptyList(),
            )
        assertThat(
            (givenGroup.toRest()).toBusiness(),
            `is`(givenGroup),
        )
    }

    @DataProvider(name = DATA_FOR_PARSE_TO_GROUP)
    fun createDataForParseToGroup() =
        arrayOf(
            arrayOf(
                false,
                "organisation1;192.168.82.1/22,192.168.82.7\norganisation2;192.68.254.*," +
                    "195.37.13.*," +
                    "195.37.209.160-191," +
                    "195.37.234.33-46," +
                    "192.68.*.*," +
                    "194.94.110-111.*",
                false,
                listOf(
                    GroupEntry(
                        organisationName = "organisation1",
                        ipAddresses = "192.168.82.1/22,192.168.82.7",
                    ),
                    GroupEntry(
                        organisationName = "organisation2",
                        ipAddresses = "192.68.254.*,195.37.13.*,195.37.209.160-191,195.37.234.33-46,192.68.*.*,194.94.110-111.*",
                    ),
                ),
                "All valid with every special case for IP",
            ),
            arrayOf(
                true,
                "\"Organisation\",\"IP-Address\",\"Foobar\"\n\"organisation1\",\"192.168.82.1.124\"",
                true,
                emptyList<GroupEntry>(),
                "wrong delimiter with headers leads to error",
            ),
            arrayOf(
                false,
                "organisation1;192.168.82.1",
                false,
                listOf(
                    GroupEntry(
                        organisationName = "organisation1",
                        ipAddresses = "192.168.82.1",
                    ),
                ),
                "simple case one line",
            ),
            arrayOf(
                false,
                "\n\norganisation1;192.168.82.1\norganisation2;192.168.82.1\n\n",
                false,
                listOf(
                    GroupEntry(
                        organisationName = "organisation1",
                        ipAddresses = "192.168.82.1",
                    ),
                    GroupEntry(
                        organisationName = "organisation2",
                        ipAddresses = "192.168.82.1",
                    ),
                ),
                "empty newline at the end",
            ),
            arrayOf(
                false,
                "organisation1;",
                true,
                listOf(
                    GroupEntry(
                        organisationName = "organisation1",
                        ipAddresses = "",
                    ),
                ),
                "IP address is missing",
            ),
            arrayOf(
                false,
                "",
                false,
                emptyList<GroupEntry>(),
                "Nothing to parse",
            ),
            arrayOf(
                false,
                "organisation1;192.168.82.1\norganisation2;192.168.82.1;",
                false,
                listOf(
                    GroupEntry(
                        organisationName = "organisation1",
                        ipAddresses = "192.168.82.1",
                    ),
                    GroupEntry(
                        organisationName = "organisation2",
                        ipAddresses = "192.168.82.1",
                    ),
                ),
                "parse correct even with trailing comma",
            ),
            arrayOf(
                true,
                "\nOrganisation;IP-Address\norganisation1;192.168.82.1\norganisation2;192.168.82.1\n\n",
                false,
                listOf(
                    GroupEntry(
                        organisationName = "organisation1",
                        ipAddresses = "192.168.82.1",
                    ),
                    GroupEntry(
                        organisationName = "organisation2",
                        ipAddresses = "192.168.82.1",
                    ),
                ),
                "with header line",
            ),
            arrayOf(
                true,
                "\nOrganisation,IP-Address\norganisation1,192.168.82.1\norganisation2,192.168.82.1\n\n",
                true,
                emptyList<GroupEntry>(),
                "error due to wrong separator",
            ),
            arrayOf(
                true,
                "\nOrganisation;IP-Address;Foobar\norganisation1;192.168.82.1;124\norganisation2;192.168.82.1;1234\n\n",
                false,
                listOf(
                    GroupEntry(
                        organisationName = "organisation1",
                        ipAddresses = "192.168.82.1",
                    ),
                    GroupEntry(
                        organisationName = "organisation2",
                        ipAddresses = "192.168.82.1",
                    ),
                ),
                "more columns than expected will be accepted as well.",
            ),
        )

    @Test(dataProvider = DATA_FOR_PARSE_TO_GROUP)
    fun testParseToGroup(
        hasCSVHeader: Boolean,
        ipAddressesCSV: String,
        expectsError: Boolean,
        expected: List<GroupEntry>,
        description: String,
    ) {
        if (expectsError) {
            try {
                assertThat(
                    description,
                    RestConverter.parseToGroup(
                        hasCSVHeader,
                        ipAddressesCSV,
                    ),
                    `is`(
                        expected,
                    ),
                )
                Assert.fail()
            } catch (_: IllegalArgumentException) {
            } catch (_: InvalidIPAddressException) {
            }
        } else {
            assertThat(
                description,
                RestConverter.parseToGroup(
                    hasCSVHeader,
                    ipAddressesCSV,
                ),
                `is`(
                    expected,
                ),
            )
        }
    }

    @Test
    fun testBookmarkConversion() {
        assertThat(
            TEST_BOOKMARK.copy(rightIdFilter = RightIdFilter(rightIds = listOf("1", "2"))).toString(),
            `is`(
                TEST_BOOKMARK
                    .toRest(
                        "",
                        listOf(
                            RightIdTemplateName(
                                rightId = "1",
                                templateName = "foo",
                            ),
                            RightIdTemplateName(
                                rightId = "2",
                                templateName = "bar",
                            ),
                        ),
                    ).toBusiness()
                    .toString(),
            ),
        )
    }

    @Test
    fun testSearchQuery2ItemInformation() {
        val givenItem =
            Item(
                metadata = TEST_METADATA,
                rights = listOf(TEST_RIGHT),
            )
        val given =
            SearchQueryResult(
                numberOfResults = 2,
                results =
                    listOf(
                        givenItem,
                    ),
                accessState =
                    mapOf(
                        AccessState.OPEN to 2,
                    ),
                licenceContracts = 0,
                ccLicenceNoRestrictions = 10,
                zbwUserAgreements = 0,
                paketSigels = mapOf("sigel1" to 1),
                publicationType = mapOf(PublicationType.BOOK to 1, PublicationType.THESIS to 1),
                templateNamesToOcc = mapOf("1" to ("name" to 2)),
                zdbIds = mapOf("zdb1" to 1),
                isPartOfSeries = mapOf("series1" to 1),
                filtersAsQuery = "foobar",
                licenceUrl = mapOf("by/3.0/au" to 5),
                noLegalRisks = 10,
            )
        val expected =
            ItemInformation(
                itemArray = listOf(givenItem.toRest()),
                totalPages = 2,
                accessStateWithCount =
                    listOf(
                        AccessStateWithCountRest(AccessState.OPEN.toRest(), 2),
                    ),
                licenceContracts = given.licenceContracts,
                ccLicenceNoRestrictions = given.ccLicenceNoRestrictions,
                zbwUserAgreements = given.zbwUserAgreements,
                noLegalRisks = given.noLegalRisks,
                numberOfResults = given.numberOfResults,
                paketSigelWithCount =
                    listOf(
                        PaketSigelWithCountRest(count = 1, paketSigel = "sigel1"),
                    ),
                publicationTypeWithCount =
                    listOf(
                        PublicationTypeWithCountRest(
                            count = 1,
                            publicationType = PublicationType.BOOK.toRest(),
                        ),
                        PublicationTypeWithCountRest(
                            count = 1,
                            publicationType = PublicationType.THESIS.toRest(),
                        ),
                    ),
                zdbIdWithCount =
                    listOf(
                        ZdbIdWithCountRest(
                            count = 1,
                            zdbId = "zdb1",
                        ),
                    ),
                templateNameWithCount =
                    listOf(
                        TemplateNameWithCountRest(
                            count = 2,
                            templateName = "name",
                            rightId = "1",
                        ),
                    ),
                isPartOfSeriesCount =
                    listOf(
                        IsPartOfSeriesCountRest(
                            count = 1,
                            series = "series1",
                        ),
                    ),
                filtersAsQuery = "foobar",
                licenceUrlCount =
                    listOf(
                        LicenceUrlCountRest(
                            count = 5,
                            licenceUrl = "by/3.0/au",
                        ),
                    ),
            )

        assertThat(
            given.toRest(1),
            `is`(expected),
        )
    }

    @DataProvider(name = DATA_FOR_PARSE_HANDLE)
    fun createDataForParseHandle() =
        arrayOf(
            arrayOf(
                "http://hdl.handle.net/11159/848",
                "11159/848",
                "http url",
            ),
            arrayOf(
                "https://hdl.handle.net/11159/848",
                "11159/848",
                "https url",
            ),
            arrayOf(
                "/11159/848",
                "11159/848",
                "slash prefix",
            ),
            arrayOf(
                "/11159/848",
                "11159/848",
                "desired input",
            ),
        )

    @Test(dataProvider = DATA_FOR_PARSE_HANDLE)
    fun testParseHandle(
        given: String,
        expected: String,
        details: String,
    ) {
        assertThat(
            details,
            RestConverter.parseHandle(given),
            `is`(expected),
        )
    }

    @Test
    fun testTemplateApplicationConversion() {
        // given
        val example: TemplateApplicationResult =
            TEST_TEMPLATE_APPLICATION_RESULT.copy(
                exceptionTemplateApplicationResult =
                    TEST_TEMPLATE_APPLICATION_RESULT.copy(rightId = "6", templateName = "exc"),
            )
        val expected =
            TemplateApplicationRest(
                handles = TEST_TEMPLATE_APPLICATION_RESULT.appliedMetadataHandles,
                rightId = TEST_TEMPLATE_APPLICATION_RESULT.rightId,
                templateName = TEST_TEMPLATE_APPLICATION_RESULT.templateName,
                errors = TEST_TEMPLATE_APPLICATION_RESULT.errors.map { it.toRest() },
                numberOfErrors = TEST_TEMPLATE_APPLICATION_RESULT.numberOfErrors,
                numberOfAppliedEntries = TEST_TEMPLATE_APPLICATION_RESULT.appliedMetadataHandles.size,
                testId = TEST_TEMPLATE_APPLICATION_RESULT.testId,
                exceptionTemplateApplication =
                    TEST_TEMPLATE_APPLICATION_RESULT.copy(rightId = "6", templateName = "exc").toRest(),
            )

        // when
        val received = example.toRest()

        // then
        assertThat(
            received,
            `is`(expected),
        )
    }

    companion object {
        const val DATA_FOR_PARSE_TO_GROUP = "DATA_FOR_PARSE_TO_GROUP"
        const val DATA_FOR_PARSE_HANDLE = "DATA_FOR_PARSE_HANDLE"
        val TODAY: LocalDate = LocalDate.of(2022, 3, 1)
        val TEST_METADATA =
            ItemMetadata(
                author = "Colbjørnsen, Terje",
                band = "band",
                collectionHandle = "handleCol",
                collectionName = "Collectioname",
                communityHandle = "handleCom",
                communityName = "Communityname",
                createdBy = "user1",
                createdOn =
                    OffsetDateTime.of(
                        2022,
                        3,
                        1,
                        1,
                        1,
                        0,
                        0,
                        ZoneOffset.UTC,
                    ),
                deleted = false,
                doi = listOf("10.0002", "10.982301"),
                handle = "hdl:example.handle.net",
                isbn = listOf("1234567", "890123"),
                issn = "123456",
                isPartOfSeries = listOf("seriespart"),
                lastUpdatedBy = "user2",
                lastUpdatedOn =
                    OffsetDateTime.of(
                        2022,
                        3,
                        2,
                        1,
                        1,
                        0,
                        0,
                        ZoneOffset.UTC,
                    ),
                licenceUrl = "https://creativecommons.org/licenses/by-sa/4.0/legalcode.de",
                licenceUrlFilter = "by-sa/4.0/legalcode.de",
                paketSigel = listOf("sigel"),
                ppn = "ppn",
                publicationType = PublicationType.BOOK,
                publicationYear = 2022,
                storageDate = OffsetDateTime.now(),
                subCommunityHandle = "11159/1114",
                subCommunityName = "Department",
                title = "Important title",
                titleJournal = null,
                titleSeries = null,
                zdbIds = listOf("zdbIds"),
            )

        val TEST_RIGHT =
            ItemRight(
                rightId = "123",
                accessState = AccessState.CLOSED,
                basisAccessState = BasisAccessState.LICENCE_CONTRACT,
                basisStorage = BasisStorage.AUTHOR_RIGHT_EXCEPTION,
                createdBy = "user1",
                createdOn =
                    OffsetDateTime.of(
                        2022,
                        3,
                        1,
                        1,
                        1,
                        0,
                        0,
                        ZoneOffset.UTC,
                    ),
                endDate = TODAY,
                exceptionOfId = null,
                groups =
                    listOf(
                        Group(
                            groupId = 1,
                            version = 10,
                            description = "foobar",
                            entries =
                                listOf(
                                    GroupEntry(
                                        organisationName = "blablub",
                                        ipAddresses = "127.0.0.1",
                                    ),
                                ),
                            title = "some tilte",
                            createdBy = "user1",
                            lastUpdatedBy = "user 2",
                            createdOn =
                                OffsetDateTime.of(
                                    2022,
                                    3,
                                    4,
                                    1,
                                    1,
                                    0,
                                    0,
                                    ZoneOffset.UTC,
                                ),
                            lastUpdatedOn =
                                OffsetDateTime.of(
                                    2022,
                                    3,
                                    4,
                                    1,
                                    1,
                                    0,
                                    0,
                                    ZoneOffset.UTC,
                                ),
                            oldVersions = emptyList(),
                        ),
                    ),
                groupIds = listOf(1),
                hasExceptionId = "5",
                hasLegalRisk = true,
                isTemplate = true,
                lastAppliedOn =
                    OffsetDateTime.of(
                        2022,
                        3,
                        4,
                        1,
                        1,
                        0,
                        0,
                        ZoneOffset.UTC,
                    ),
                lastUpdatedBy = "user2",
                lastUpdatedOn =
                    OffsetDateTime.of(
                        2022,
                        3,
                        2,
                        1,
                        1,
                        0,
                        0,
                        ZoneOffset.UTC,
                    ),
                startDate = TODAY.minusDays(1),
                licenceContract = "some contract",
                notesGeneral = "Some general notes",
                notesFormalRules = "Some formal rule notes",
                notesProcessDocumentation = "Some process documentation",
                notesManagementRelated = "Some management related notes",
                predecessorId = null,
                restrictedOpenContentLicence = false,
                successorId = null,
                templateDescription = "foo",
                templateName = "name",
                zbwUserAgreement = true,
            )

        val TEST_DA_ITEM =
            DAItem(
                id = 5,
                name = "name",
                handle = "http://hdl.handle.net/11159/848",
                type = "type",
                link = "link",
                expand = listOf("foo"),
                lastModified = "2020-10-04",
                parentCollection =
                    DACollection(
                        id = 3,
                        name = "Collectionname",
                        handle = "http://hdl.handle.net/11159/849",
                        type = null,
                        link = "link",
                        expand = emptyList(),
                        logo = null,
                        parentCommunity = null,
                        copyrightText = null,
                        introductoryText = null,
                        shortDescription = null,
                        sidebarText = null,
                        items = emptyList(),
                        license = null,
                        numberItems = 4,
                        parentCommunityList = emptyList(),
                    ),
                parentCollectionList = emptyList(),
                parentCommunityList =
                    listOf(
                        DACommunity(
                            id = 1,
                            name = "Communityname",
                            handle = "http://hdl.handle.net/11159/850",
                            type = null,
                            countItems = null,
                            link = "link",
                            expand = emptyList(),
                            logo = null,
                            parentCommunity = null,
                            copyrightText = null,
                            introductoryText = null,
                            shortDescription = null,
                            sidebarText = null,
                            subcommunities = emptyList(),
                            collections = emptyList(),
                        ),
                        DACommunity(
                            id = 2,
                            name = "Department",
                            handle = "http://hdl.handle.net/11159/1114",
                            type = null,
                            countItems = null,
                            link = "link",
                            expand = emptyList(),
                            logo = null,
                            parentCommunity = null,
                            copyrightText = null,
                            introductoryText = null,
                            shortDescription = null,
                            sidebarText = null,
                            subcommunities = emptyList(),
                            collections = emptyList(),
                        ),
                    ),
                metadata =
                    listOf(
                        DAMetadata(
                            key = "dc.identifier.uri",
                            value = "http://hdl.handle.net/11159/848",
                            language = "DE",
                        ),
                        DAMetadata(
                            key = "dc.type",
                            value = "article",
                            language = "DE",
                        ),
                        DAMetadata(
                            key = "dc.contributor.author",
                            value = "Colbjørnsen, Terje",
                            language = null,
                        ),
                        DAMetadata(
                            key = "dc.date.issued",
                            value = "2022-09",
                            language = "DE",
                        ),
                        DAMetadata(
                            key = "dc.date.accessioned",
                            value = "2022-01-19T07:57:26Z",
                            language = "DE",
                        ),
                        DAMetadata(
                            key = "dc.title",
                            value = "some_title",
                            language = "DE",
                        ),
                        DAMetadata(
                            key = "dc.journalname",
                            value = "some_journal",
                            language = "DE",
                        ),
                        DAMetadata(
                            key = "dc.seriesname",
                            value = "some_series",
                            language = "DE",
                        ),
                        DAMetadata(
                            key = "dc.rights.license",
                            value = "https://creativecommons.org/licenses/by-sa/4.0/legalcode.de",
                            language = "EN",
                        ),
                        DAMetadata(
                            key = "dc.relation.ispartofseries",
                            value = "seriespart",
                            language = "EN",
                        ),
                        DAMetadata(
                            key = "dc.relation.journalzdbid",
                            value = "zdbId1",
                            language = "EN",
                        ),
                        DAMetadata(
                            key = "dc.relation.serieszdbid",
                            value = "zdbId2",
                            language = "EN",
                        ),
                        DAMetadata(
                            key = "dc.identifier.pi",
                            value = "10.7298/c5ps-be97",
                            language = "EN",
                        ),
                        DAMetadata(
                            key = "dc.identifier.pi",
                            value = "1813/110555",
                            language = "EN",
                        ),
                        DAMetadata(
                            key = "dc.identifier.isbn",
                            value = "9781847200235",
                            language = "EN",
                        ),
                        DAMetadata(
                            key = "dc.identifier.isbn",
                            value = "9781845420680",
                            language = "EN",
                        ),
                    ),
                bitstreams = emptyList(),
                archived = "archived",
                withdrawn = "withdrawn",
            )

        val TEST_BOOKMARK =
            Bookmark(
                bookmarkId = 1,
                bookmarkName = "test",
                description = "some description",
                searchTerm = "tit:someTitle",
                publicationYearFilter = QueryParameterParser.parsePublicationYearFilter("2020-2030"),
                publicationTypeFilter = QueryParameterParser.parsePublicationTypeFilter("BOOK,ARTICLE"),
                accessStateFilter = QueryParameterParser.parseAccessStateFilter("OPEN,RESTRICTED"),
                validOnFilter = QueryParameterParser.parseRightValidOnFilter("2018-04-01"),
                startDateFilter = QueryParameterParser.parseStartDateFilter("2020-01-01"),
                endDateFilter = QueryParameterParser.parseEndDateFilter("2021-12-31"),
                formalRuleFilter = QueryParameterParser.parseFormalRuleFilter("ZBW_USER_AGREEMENT"),
                paketSigelFilter = QueryParameterParser.parsePaketSigelFilter("sigel"),
                zdbIdFilter = QueryParameterParser.parseZDBIdFilter("zdbId1,zdbId2"),
                noRightInformationFilter = QueryParameterParser.parseNoRightInformationFilter("false"),
                manualRightFilter = QueryParameterParser.parseManualRightFilter("true"),
                accessStateOnFilter = QueryParameterParser.parseAccessStateOnDate("OPEN+2024-09-17"),
                lastUpdatedOn =
                    OffsetDateTime.of(
                        2022,
                        3,
                        2,
                        1,
                        1,
                        0,
                        0,
                        ZoneOffset.UTC,
                    ),
                lastUpdatedBy = "user2",
                createdBy = "user1",
                createdOn =
                    OffsetDateTime.of(
                        2022,
                        3,
                        2,
                        1,
                        1,
                        0,
                        0,
                        ZoneOffset.UTC,
                    ),
            )

        val TEST_TEMPLATE_APPLICATION_RESULT =
            TemplateApplicationResult(
                rightId = "5",
                templateName = "parent",
                testId = "foobar",
                appliedMetadataHandles = listOf("one", "two"),
                errors =
                    listOf(
                        RightError(
                            errorId = 1,
                            message = "Timing conflict",
                            conflictingWithRightId = "sourceRightId",
                            conflictByRightId = "conflictingRightId",
                            handle = "somehandle",
                            createdOn = ErrorRoutesKtTest.Companion.NOW,
                            conflictType = ConflictType.DATE_OVERLAP,
                            conflictByContext = "template name",
                            testId = null,
                            createdBy = "user1",
                        ),
                    ),
                numberOfErrors = 1,
                exceptionTemplateApplicationResult = null,
            )
    }
}

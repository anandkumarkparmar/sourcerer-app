// Copyright 2018 Sourcerer Inc. All Rights Reserved.
// Author: Alexander Surkov (alex@sourcerer.io)

package test.tests.hashers

import app.api.MockApi
import app.extractors.Extractor
import app.hashers.CommitHasher
import app.hashers.CommitCrawler
import app.model.*
import app.utils.RepoHelper
import org.eclipse.jgit.api.Git
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import test.utils.TestRepo
import java.io.File
import java.util.stream.StreamSupport.stream
import kotlin.streams.toList
import kotlin.test.assertEquals

class IgnoreVendorsTest : Spek({
    fun cleanRepos() {
        Runtime.getRuntime().exec("src/test/delete_repo.sh").waitFor()
    }

    val userName = "Contributor"
    val userEmail = "test@domain.com"

    // Creation of test repo.
    cleanRepos()

    given("Ignove vendor files") {
        val author = Author(userName, userEmail)
        val emails = hashSetOf(userEmail)

        val testRepoPath = "../IgnoreVendors_t1"
        val testRepo = TestRepo(testRepoPath)

        val testRehash = "rehash_IgnoreVendors_t1"
        val serverRepo = Repo(rehash = testRehash)

        val mockApi = MockApi(mockRepo = serverRepo)
        val observable = CommitCrawler.getObservable(testRepo.git, serverRepo)

        it("t1: JS") {
            val lines = listOf(
                "let i = 0"
            )

            testRepo.createFile("lala.js", lines)
            testRepo.createFile("pdf.worker.js", lines)
            testRepo.commit(message = "commit1", author = author)

            val errors = mutableListOf<Throwable>()
            CommitHasher(serverRepo, mockApi, listOf("rehashes"), emails)
                .updateFromObservable(observable, { e -> errors.add(e) })
            if (errors.size > 0) {
                println(errors[0].message)
            }
            assertEquals(0, errors.size)

            val mapStats = mockApi.receivedAddedCommits
                .fold(mutableListOf<CommitStats>()) { allStats, commit ->
                    allStats.addAll(commit.stats)
                    allStats
                }

            assertEquals(1, mapStats.size)
            assertEquals(1, mapStats.map { it.numLinesAdded }.sum())
            assertEquals(0, mapStats.map { it.numLinesDeleted }.sum())
        }

        afterGroup {
            testRepo.destroy()
        }
    }

    cleanRepos()
})

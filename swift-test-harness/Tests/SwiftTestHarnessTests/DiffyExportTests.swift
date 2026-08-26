#if canImport(Testing)
import Testing
import Diffy

@Suite struct DiffyExportTests {
    @Test func testSwiftModuleLoads() throws {
        _ = diff.DiffOptions()
        #expect(Bool(true), "Diffy swift module imported cleanly")
    }
}
#elseif canImport(XCTest)
import XCTest
import Diffy

final class DiffyExportTests: XCTestCase {
    func testSwiftModuleLoads() throws {
        _ = diff.DiffOptions()
        XCTAssertTrue(true, "Diffy swift module imported cleanly")
    }
}
#endif

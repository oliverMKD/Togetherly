package com.togetherly.data.media

/**
 * Opaque handle to a just-picked photo, produced only by platform picker code and consumed only
 * by the platform [PhotoImporter] implementation — this marker carries no members of its own, so
 * a platform picker type (Android `Uri`, iOS file `NSURL`) never appears in a common function
 * signature. Each platform's own concrete class (declared in its own source set, never in
 * `commonMain`) attaches the real payload; common code only ever holds and forwards a reference.
 */
interface PhotoImportSource

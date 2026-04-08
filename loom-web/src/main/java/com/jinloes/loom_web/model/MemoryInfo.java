package com.jinloes.loom_web.model;

public record MemoryInfo(long heapUsedMb, long heapMaxMb, long jvmCommittedMb, long processRssMb) {}

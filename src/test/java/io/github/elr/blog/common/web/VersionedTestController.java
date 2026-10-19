package io.github.elr.blog.common.web;

import java.util.List;
import java.util.Set;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@TestComponent // excluded from component scanning: only tests that @Import it see this controller
@RestController
@RequestMapping(path = "/api/{version}/widgets", version = "v1") // the project convention (#15)
class VersionedTestController {

    record Widget(String name) {}

    @GetMapping
    PageResponse<Widget> list(@ParameterObject Pageable pageable) { // documented as page, size, sort
        Pageable safe = PageRequests.sanitize(pageable, Set.of("name", "createdAt"), Sort.by("name"));
        List<String> all = List.of("a", "b", "c");
        return PageResponse.from(new PageImpl<>(all, safe, all.size()), Widget::new);
    }
}

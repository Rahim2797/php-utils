<?php

use App\Support\PropertySelectionService;

(new PropertySelectionService())->acceptProductProperties([
    'sk<caret>u' => 'ABC-123',
]);
